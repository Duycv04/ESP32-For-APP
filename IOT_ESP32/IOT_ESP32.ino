#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <DHT.h>

// ==== THÔNG TIN WIFI VÀ FIREBASE ====
const char* ssid = "Redmi Note 12S";
const char* password = "88888888";

#define FIREBASE_HOST "https://esp32-demo-97f70-default-rtdb.firebaseio.com/"
#define FIREBASE_AUTH "280w2Uzvt09er4ZSL4oYKM9hWReKw5rA5roMH591"
#define API_KEY ""  // Nếu chỉ dùng RTDB thì có thể để trống

// ==== CẤU HÌNH CẢM BIẾN DHT22 ====
#define DHTPIN 4
#define DHTTYPE DHT22
DHT dht(DHTPIN, DHTTYPE);

// ==== CẤU HÌNH LED ====
int ledPins[4] = {26, 25, 33, 32}; // led1 -> led4

// ==== CẤU HÌNH FIREBASE ====
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

void setup() {
  Serial.begin(115200);
  dht.begin();

  // Kết nối WiFi
  WiFi.begin(ssid, password);
  Serial.print("Đang kết nối WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\n✅ Đã kết nối WiFi");

  // Cấu hình Firebase
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  // Cấu hình các chân LED
  for (int i = 0; i < 4; i++) {
    pinMode(ledPins[i], OUTPUT);
    digitalWrite(ledPins[i], LOW);
  }
}

void loop() {
  // ==== Đọc trạng thái LED từ Firebase và bật/tắt ====
  for (int i = 0; i < 4; i++) {
    String path = "/devices/esp32_1/leds/led" + String(i + 1);
    if (Firebase.RTDB.getBool(&fbdo, path.c_str())) {
      bool ledState = fbdo.boolData();
      digitalWrite(ledPins[i], ledState ? HIGH : LOW);
      Serial.printf("LED %d = %d\n", i + 1, ledState);
    } else {
      Serial.printf("❌ Không đọc được %s: %s\n", path.c_str(), fbdo.errorReason().c_str());
    }
  }

  // ==== Đọc nhiệt độ và độ ẩm từ DHT22 ====
  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();

  if (!isnan(temperature) && !isnan(humidity)) {
    Serial.printf("🌡️ Temp: %.2f°C | 💧 Humidity: %.2f%%\n", temperature, humidity);

    // Gửi dữ liệu lên Firebase
    Firebase.RTDB.setFloat(&fbdo, "/devices/esp32_1/sensors/temperature", temperature);
    Firebase.RTDB.setFloat(&fbdo, "/devices/esp32_1/sensors/humidity", humidity);

    // Gửi cảnh báo nếu nhiệt độ cao
    if (temperature > 35) {
      Firebase.RTDB.setString(&fbdo, "/devices/esp32_1/alert", "Cảnh báo! Nhiệt độ cao");
    } else {
      Firebase.RTDB.setString(&fbdo, "/devices/esp32_1/alert", "");
    }
  } else {
    Serial.println("❌ Lỗi đọc DHT22");
  }

  delay(2000);
}
