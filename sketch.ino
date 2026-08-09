#define BLYNK_TEMPLATE_ID "TU_TEMPLATE_ID"
#define BLYNK_TEMPLATE_NAME "TU_TEMPLATE_NAME"
#define BLYNK_AUTH_TOKEN "PEGA_AQUI_TU_TOKEN_DE_BLYNK"

#define BLYNK_PRINT Serial

#include <WiFi.h>
#include <WiFiClient.h>
#include <BlynkSimpleEsp32.h>
#include <math.h>

char auth[] = BLYNK_AUTH_TOKEN;
char ssid[] = "Wokwi-GUEST";
char pass[] = "";

// Pines
const int PIN_LED = 4;
const int PIN_POT = 34;
const int PIN_BTN = 18;  
BlynkTimer timer;

// Estado del sistema
bool enEvento = false;
int segundoEvento = 0;
int timerRafagaID = -1;


// --------------------------------------------------
// LATIDO
// --------------------------------------------------

void enviarLatido() {

  if (!enEvento) {

    Blynk.virtualWrite(V3, "reposo");

    Serial.println("[Nodo] Latido enviado: reposo");

    digitalWrite(PIN_LED, HIGH);
    delay(50);
    digitalWrite(PIN_LED, LOW);
  }
}


// --------------------------------------------------
// PROCESAR RÁFAGA DE SISMO
// --------------------------------------------------

void procesarRafagaSismo() {

  if (!enEvento) return;

  segundoEvento++;

  // Leer potenciómetro
  int lecturaRaw = analogRead(PIN_POT);

  float magnitudBase =
      (lecturaRaw / 4095.0) * 10.0;

  // Forma de la onda
  float factorForma =
      sin((segundoEvento / 15.0) * M_PI) *
      exp(-0.15 * segundoEvento);

  float lecturaAceleracion =
      magnitudBase * factorForma * 2.2;

  // Limitar entre 0 y 10
  if (lecturaAceleracion > 10.0)
    lecturaAceleracion = 10.0;

  if (lecturaAceleracion < 0.0)
    lecturaAceleracion = 0.0;

  // Enviar a Blynk
  Blynk.virtualWrite(V1, lecturaAceleracion);

  Serial.print("[Evento] Segundo ");
  Serial.print(segundoEvento);
  Serial.print("/15 -> Aceleracion: ");
  Serial.print(lecturaAceleracion);
  Serial.println(" g");

  // Terminar después de 15 lecturas
  if (segundoEvento >= 15) {
    finalizarEvento();
  }
}


// --------------------------------------------------
// ACTIVAR ALERTA
// --------------------------------------------------

void activarAlerta(String origen) {

  if (enEvento) return;

  Serial.println();
  Serial.println("--------------------------------------------");
  Serial.println("¡ALERTA DE SISMO!");
  Serial.println("Origen: " + origen);
  Serial.println("--------------------------------------------");

  enEvento = true;
  segundoEvento = 0;

  // ENCENDER LED
  digitalWrite(PIN_LED, HIGH);

  // Estado en Blynk
  Blynk.virtualWrite(V3, "evento");

  // Limpiar botón virtual
  Blynk.virtualWrite(V2, 0);

  // Iniciar ráfaga
  timerRafagaID =
      timer.setInterval(1000L, procesarRafagaSismo);
}


// --------------------------------------------------
// FINALIZAR EVENTO
// --------------------------------------------------

void finalizarEvento() {

  Serial.println(
      "Fin de la rafaga de 15s. Regresando a reposo."
  );

  // Detener temporizador
  if (timerRafagaID != -1) {

    timer.deleteTimer(timerRafagaID);

    timerRafagaID = -1;
  }

  enEvento = false;

  // APAGAR LED
  digitalWrite(PIN_LED, LOW);

  // Actualizar Blynk
  Blynk.virtualWrite(V2, 0);
  Blynk.virtualWrite(V3, "reposo");
}


// --------------------------------------------------
// BLYNK CONECTADO
// --------------------------------------------------

BLYNK_CONNECTED() {

  Blynk.syncVirtual(V2);
}


// --------------------------------------------------
// BOTÓN VIRTUAL BLYNK
// --------------------------------------------------

BLYNK_WRITE(V2) {

  int valorV2 = param.asInt();

  if (valorV2 == 1 && !enEvento) {

    activarAlerta("App Mobile / Blynk Cloud");
  }
}


// --------------------------------------------------
// SETUP
// --------------------------------------------------

void setup() {

  Serial.begin(115200);

  // LED
  pinMode(PIN_LED, OUTPUT);
  digitalWrite(PIN_LED, LOW);

  // BOTÓN
  pinMode(PIN_BTN, INPUT_PULLUP);

  Serial.println();
  Serial.println("Conectando a WiFi...");

  // WiFi Wokwi
  WiFi.begin(ssid, pass, 6);

  while (WiFi.status() != WL_CONNECTED) {

    delay(250);

    Serial.print(".");
  }

  Serial.println();
  Serial.println(
      "WiFi conectado. IP: " +
      WiFi.localIP().toString()
  );

  // Blynk
  Serial.println(
      "Conectando a los servidores de Blynk..."
  );

  Blynk.config(auth);
  Blynk.connect();

  if (Blynk.connected()) {

    Serial.println(
        "¡Conectado a Blynk exitosamente!"
    );

  } else {

    Serial.println(
        "Fallo al conectar con Blynk."
    );
  }

  // Latido cada 60 segundos
  timer.setInterval(60000L, enviarLatido);

  // Estados iniciales
  Blynk.virtualWrite(V1, 0.0);
  Blynk.virtualWrite(V2, 0);
  Blynk.virtualWrite(V3, "reposo");
}


// --------------------------------------------------
// LOOP
// --------------------------------------------------

void loop() {

  Blynk.run();
  timer.run();

  // Leer botón físico
  if (digitalRead(PIN_BTN) == LOW) {

    delay(50); // Antirrebote

    if (digitalRead(PIN_BTN) == LOW && !enEvento) {

      activarAlerta("Boton Fisico Wokwi");

      // Esperar a que se suelte
      while (digitalRead(PIN_BTN) == LOW) {

        Blynk.run();
        timer.run();
        delay(10);
      }
    }
  }
}
