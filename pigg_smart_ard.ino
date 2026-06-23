

//НАСТРОЙКИ
#define coin_amount 5    // число монет, которые нужно распознать
float coin_value[coin_amount] = {0.5, 1.0, 2.0, 5.0, 10.0};  // стоимость монет
String currency = "RUB"; // валюта 
int stb_time = 10000;    // время в сон

int coin_signal[coin_amount];    // значение сигнала для каждого размера монет
int coin_quantity[coin_amount];  // количество монет
byte empty_signal;               // пустой сигнал
unsigned long standby_timer, reset_timer; // таймеры
float summ_money = 0;            // сумма монет в копилке

#include "LowPower.h"
#include "EEPROMex.h"
#include "LCD_1602_RUS.h"

LCD_1602_RUS lcd(0x27, 16, 2);            

boolean recogn_flag, sleep_flag = true;   
//КНОПКИ
#define button 2         // кнопка "проснуться"
#define calibr_button 3  // калибровки и сброса
#define disp_power 12    // питание дисплея
#define LEDpin 11        // питание светодиода
#define IRpin 17         // питание фототранзистора
#define IRsens 14        // сигнал фототранзистора

int sens_signal, last_sens_signal;
boolean coin_flag = false;

void setup() {
  Serial.begin(9600);                 
  delay(500);

  // кнопки
  pinMode(button, INPUT_PULLUP);
  pinMode(calibr_button, INPUT_PULLUP);

  // пины питания как выходы
  pinMode(disp_power, OUTPUT);
  pinMode(LEDpin, OUTPUT);
  pinMode(IRpin, OUTPUT);

  // питание на дисплей и датчик
  digitalWrite(disp_power, 1);
  digitalWrite(LEDpin, 1);
  digitalWrite(IRpin, 1);

  // прерывание
  attachInterrupt(0, wake_up, CHANGE);

  empty_signal = analogRead(IRsens);  //  пустой сигнал

  // дисплей
  lcd.init();
  lcd.backlight();

  if (!digitalRead(calibr_button)) {  // если при запуске нажата кнопка калибровки
    lcd.clear();
    lcd.setCursor(3, 0);
    lcd.print(L"Сервис");
    delay(500);
    reset_timer = millis();
    while (1) {                                   
      if (millis() - reset_timer > 3000) {        // если кнопка всё ещё удерживается и прошло 3 секунды
        // очистить количество монет
        for (byte i = 0; i < coin_amount; i++) {
          coin_quantity[i] = 0;
          EEPROM.writeInt(20 + i * 2, 0);
        }
        lcd.clear();
        lcd.setCursor(0, 0);
        lcd.print(L"Память очищена");
        delay(100);
      }
      if (digitalRead(calibr_button)) {   // если отпустили кнопку, перейти к калибровке
        lcd.clear();
        lcd.setCursor(0, 0);
        lcd.print(L"Калибровка");
        break;
      }
    }
    while (1) {
      for (byte i = 0; i < coin_amount; i++) {
        lcd.setCursor(0, 1); lcd.print(coin_value[i]);  // отобразить цену монеты, которая калибруется
        lcd.setCursor(13, 1); lcd.print(currency);      // отобразить валюту
        last_sens_signal = empty_signal;
        while (1) {
          sens_signal = analogRead(IRsens);                                    // считать датчик
          if (sens_signal > last_sens_signal) last_sens_signal = sens_signal;  // если значение больше предыдущего
          if (sens_signal - empty_signal > 3) coin_flag = true;                // если значение упало почти до "пустого", считать что монета улетела
          if (coin_flag && (abs(sens_signal - empty_signal)) < 2) {            // если монета точно улетела
            coin_signal[i] = last_sens_signal;                                 // записать максимальное значение в память
            EEPROM.writeInt(i * 2, coin_signal[i]);
            coin_flag = false;
            break;
          }
        }
      }
      break;
    }
  }

  // при старте считать из памяти сигналы монет 
  for (byte i = 0; i < coin_amount; i++) {
    coin_signal[i] = EEPROM.readInt(i * 2);
    coin_quantity[i] = EEPROM.readInt(20 + i * 2);
    summ_money += coin_quantity[i] * coin_value[i];  // и сумму сразу посчитать, как произведение цены монеты на количество
  }

 
  standby_timer = millis();  // обнулить таймер ухода в сон
}

void loop() {
  if (sleep_flag) {  // если включение  после сна, грузить дисплей и вывести текст
    delay(500);
    lcd.init();
    lcd.clear();
    lcd.setCursor(0, 0); lcd.print(L"На яхту");
    lcd.setCursor(0, 1); lcd.print(summ_money);
    lcd.setCursor(13, 1); lcd.print(currency);
    empty_signal = analogRead(IRsens);
    sleep_flag = false;
  }

  // далее в бесконечном цикле
  last_sens_signal = empty_signal;
  while (1) {
    sens_signal = analogRead(IRsens);  // далее такой же алгоритм, как при калибровке
    if (sens_signal > last_sens_signal) last_sens_signal = sens_signal;
    if (sens_signal - empty_signal > 3) coin_flag = true;
    if (coin_flag && (abs(sens_signal - empty_signal)) < 2) {
      recogn_flag = false;  
      // нашли максимум для пролетевшей монетки, запись в last_sens_signal
      // далее начинаем сравнивать со значениями для монет, хранящимися в памяти
      for (byte i = 0; i < coin_amount; i++) {
        int delta = abs(last_sens_signal - coin_signal[i]);   // ищем абсолютное
        // значение разности полученного сигнала с нашими значениями из памяти
        if (delta < 30) {   // если эта разность попадает в диапазон, то считаем монетку распознанной
          summ_money += coin_value[i];  // к сумме прибавляем цену 
          lcd.setCursor(0, 1); lcd.print(summ_money);
          coin_quantity[i]++;  // для распознанного номера монетки прибавляем количество
          recogn_flag = true;
          break;
        }
      }
      coin_flag = false;
      standby_timer = millis();  // сбросить таймер
      break;
    }

    // если ничего не делали, время таймера вышло, уходит в сон
    if (millis() - standby_timer > stb_time) {
      good_night();
      break;
    }

    // если монетка вставлена и удерживается 2 секунды
    while (!digitalRead(button)) {
      if (millis() - standby_timer > 2000) {
        lcd.clear();

        // отобразить на дисплее: сверху цены монет, снизу их количество
        for (byte i = 0; i < coin_amount; i++) {
          lcd.setCursor(i * 3, 0); lcd.print((int)coin_value[i]);
          lcd.setCursor(i * 3, 1); lcd.print(coin_quantity[i]);
        }
      }
    }
  }
}

// функция сна
void good_night() {
  // перед тем как пойти в сон, записываем в EEPROM новые полученные количества монет по адресам начиная с 20го 
  for (byte i = 0; i < coin_amount; i++) {
    EEPROM.updateInt(20 + i * 2, coin_quantity[i]);
  }
  sleep_flag = true;
  // вырубить питание со всех дисплеев и датчиков
  digitalWrite(disp_power, 0);
  digitalWrite(LEDpin, 0);
  digitalWrite(IRpin, 0);
  delay(100);
  // и вот теперь в сон
  LowPower.powerDown(SLEEP_FOREVER, ADC_OFF, BOD_OFF);
}

// просыпаемся по прерыванию 
void wake_up() {
  // возвращаем питание на дисплей и датчик
  digitalWrite(disp_power, 1);
  digitalWrite(LEDpin, 1);
  digitalWrite(IRpin, 1);
  standby_timer = millis();  // и обнуляем таймер
}

