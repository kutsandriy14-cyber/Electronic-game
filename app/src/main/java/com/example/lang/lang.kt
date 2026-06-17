package com.example.lang

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.model.ComponentCategory
import com.example.model.ComponentType
import java.io.BufferedReader
import java.io.InputStreamReader

enum class AppLanguage(val code: String, val displayName: String) {
    EN("en", "English"),
    RU("ru", "Русский"),
    UK("uk", "Українська")
}

object Lang {
    private val translations = java.util.concurrent.ConcurrentHashMap<AppLanguage, Map<String, String>>()

    fun init(context: Context) {
        for (lang in AppLanguage.values()) {
            val map = mutableMapOf<String, String>()
            try {
                val fileName = "lang${lang.code.lowercase()}.txt"
                context.assets.open(fileName).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val trimmed = line!!.trim()
                            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                            val eq = trimmed.indexOf('=')
                            if (eq != -1) {
                                val key = trimmed.substring(0, eq).trim()
                                val value = trimmed.substring(eq + 1).trim()
                                map[key] = value
                            }
                        }
                    }
                }
                translations[lang] = map
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun t(key: String, lang: AppLanguage): String {
        return translations[lang]?.get(key) ?: fallbackTranslations[lang]?.get(key) ?: fallbackTranslations[AppLanguage.EN]?.get(key) ?: key
    }

    // Static fallback dictionary in case dynamic assets haven't initialized yet
    private val fallbackTranslations = mapOf(
        AppLanguage.EN to mapOf(
            "app_name" to "ElectroSim",
            "simulation" to "Simulation",
            "clear" to "Clear",
            "save" to "Save",
            "load" to "Load",
            "settings" to "Settings",
            "tools_modes" to "Tools & Modes",
            "power_sources" to "Power Sources",
            "conveyance" to "Conveyance",
            "switches_inputs" to "Switches & Inputs",
            "sensors" to "Sensors",
            "outputs_actuators" to "Outputs & Actuators",
            "logic_gates" to "Logic & Gates",
            "analog_ics" to "Analog ICs",
            "advanced_memory" to "Advanced & MCU",
            "materials_fluids" to "Materials & Solids",
            "hydraulics" to "Fluids & Gases",
            "telemetry" to "Telemetry",
            "voltage" to "Voltage",
            "current" to "Current",
            "power" to "Power",
            "short_circuit" to "SHORT CIRCUIT!",
            "active_scripts" to "Active Scripts",
            "mcu_logs" to "MCU LOGS (SSM)",
            "inspect" to "Inspect Component",
            "multimeter" to "Multimeter",
            "save_dialog_title" to "Save Schematic",
            "load_dialog_title" to "Load Schematic",
            "settings_dialog_title" to "Simulation Settings",
            "grid_size" to "Grid Size",
            "resize" to "Resize",
            "import_export" to "Scheme Code",
            "copy" to "Copy Code",
            "paste" to "Import Code",
            "delete" to "Delete",
            "cancel" to "Cancel",
            "close" to "Close",
            "edit_properties" to "Edit Properties",
            "recharge_repair" to "Recharge & Repair",
            "apply" to "Apply",
            "live_state" to "Live State",
            "schemes_empty" to "No saved schemes yet",
            "enter_name" to "Enter blueprint name",
            "language" to "App Language",
            "simulated_ram" to "Hardware RAM Allocation (GB)",
            "simulated_cores" to "Simulation CPU Core Count",
            "simulated_mhz" to "Clock Frequency Throttling (MHz)",
            "canvas_style" to "Canvas Visual Style",
            "system_limits" to "Simulated Device Limits",
            "canvas_classic" to "Cosmic Neon Slate",
            "canvas_blueprint" to "Lab Drafting Blueprint",
            "canvas_oled" to "Power-Saving OLED Black"
        ),
        AppLanguage.RU to mapOf(
            "app_name" to "ЭлектроСим",
            "simulation" to "Симуляция",
            "clear" to "Очистить",
            "save" to "Сохранить",
            "load" to "Загрузить",
            "settings" to "Свойства",
            "tools_modes" to "Приборы и Управление",
            "power_sources" to "Питание",
            "conveyance" to "Провода и Резисторы",
            "switches_inputs" to "Кнопки и Реле",
            "sensors" to "Датчики",
            "outputs_actuators" to "Лампы, Двигатели",
            "logic_gates" to "Логика и ИМС",
            "analog_ics" to "Аналоговые ИС",
            "advanced_memory" to "Контроллеры",
            "materials_fluids" to "Твердые блоки",
            "hydraulics" to "Жидкости и Газы",
            "telemetry" to "Телеметрия",
            "voltage" to "Напряжение",
            "current" to "Ток",
            "power" to "Мощность",
            "short_circuit" to "КОРОТКОЕ ЗАМЫКАНИЕ!",
            "active_scripts" to "Активные скрипты",
            "mcu_logs" to "Логи МК (SSM)",
            "inspect" to "Осмотр детали",
            "multimeter" to "Мультиметр",
            "save_dialog_title" to "Сохранить схему",
            "load_dialog_title" to "Загрузить схему",
            "settings_dialog_title" to "Настройки симулятора",
            "grid_size" to "Размер поля",
            "resize" to "Изменить",
            "import_export" to "Код чертежа",
            "copy" to "Копировать",
            "paste" to "Импортировать",
            "delete" to "Удалить",
            "cancel" to "Отмена",
            "close" to "Закрыть",
            "edit_properties" to "Свойства элемента",
            "recharge_repair" to "Подзарядить / Починить",
            "apply" to "Применить",
            "live_state" to "Состояние",
            "schemes_empty" to "Нет сохраненных схем",
            "enter_name" to "Введите название схемы",
            "language" to "Язык интерфейса",
            "simulated_ram" to "Выделение ОЗУ устройства (ГБ)",
            "simulated_cores" to "Ядра процессора для симуляции",
            "simulated_mhz" to "Лимит тактовой частоты (МГц)",
            "canvas_style" to "Стиль полотна подложки",
            "system_limits" to "Лимиты ресурсов (имитация телефона)",
            "canvas_classic" to "Космический Неоновый Сланец",
            "canvas_blueprint" to "Инженерный Синий Чертеж",
            "canvas_oled" to "Черно-белый OLED эконом"
        ),
        AppLanguage.UK to mapOf(
            "app_name" to "ЕлектроСим",
            "simulation" to "Симуляція",
            "clear" to "Очистити",
            "save" to "Зберегти",
            "load" to "Завантажити",
            "settings" to "Налаштування",
            "tools_modes" to "Прилади та Режими",
            "power_sources" to "Живлення",
            "conveyance" to "Провідники",
            "switches_inputs" to "Кнопки та Реле",
            "sensors" to "Датчики",
            "outputs_actuators" to "Лампи та Двигуни",
            "logic_gates" to "Логіка та Елементи",
            "analog_ics" to "Аналогові ІС",
            "advanced_memory" to "Контролери та Пам'ять",
            "materials_fluids" to "Тверді блоки",
            "hydraulics" to "Рідини та Гази",
            "telemetry" to "Телеметрія",
            "voltage" to "Напруга",
            "current" to "Струм",
            "power" to "Потужність",
            "short_circuit" to "КОРОТКЕ ЗАМЫКАНИЕ!",
            "active_scripts" to "Активні скрипти",
            "mcu_logs" to "Логи МК (SSM)",
            "inspect" to "Огляд елемента",
            "multimeter" to "Мультиметр",
            "save_dialog_title" to "Зберегти схему",
            "load_dialog_title" to "Завантажити схему",
            "settings_dialog_title" to "Налаштування симулятора",
            "grid_size" to "Розмір поля",
            "resize" to "Змінити",
            "import_export" to "Код схеми",
            "copy" to "Копіювати",
            "paste" to "Імпортувати",
            "delete" to "Вилучити",
            "cancel" to "Скасувати",
            "close" to "Закрити",
            "edit_properties" to "Властивості елемента",
            "recharge_repair" to "Підзарядити / Полагодити",
            "apply" to "Застосувати",
            "live_state" to "Стан",
            "schemes_empty" to "Немає збережених схем",
            "enter_name" to "Введіть назву схеми",
            "language" to "Мова інтерфейсу",
            "simulated_ram" to "Виділення ОЗП пристрою (ГБ)",
            "simulated_cores" to "Ядра процесора для симуляції",
            "simulated_mhz" to "Частота тактового генератора (МГц)",
            "canvas_style" to "Стиль фонового полотна",
            "system_limits" to "Обмеження ресурсів (імітація телефону)",
            "canvas_classic" to "Космічний Неоновий Сланець",
            "canvas_blueprint" to "Інженерний Синій Кресляр",
            "canvas_oled" to "Чорно-білий OLED економ"
        )
    )

    fun getCategoryName(category: ComponentCategory, lang: AppLanguage): String {
        return when (category) {
            ComponentCategory.TOOLS -> t("tools_modes", lang)
            ComponentCategory.POWER -> t("power_sources", lang)
            ComponentCategory.CONDUCTORS -> t("conveyance", lang)
            ComponentCategory.SWITCHES -> t("switches_inputs", lang)
            ComponentCategory.SENSORS -> t("sensors", lang)
            ComponentCategory.OUTPUTS -> t("outputs_actuators", lang)
            ComponentCategory.LOGIC -> t("logic_gates", lang)
            ComponentCategory.ANALOG_ICS -> t("analog_ics", lang)
            ComponentCategory.ADVANCED -> t("advanced_memory", lang)
            ComponentCategory.HYDRAULICS -> t("hydraulics", lang)
            ComponentCategory.MATERIALS -> t("materials_fluids", lang)
        }
    }

    fun getComponentDisplayName(type: ComponentType, lang: AppLanguage): String {
        if (lang == AppLanguage.EN) {
            return type.name.replace("_ANY", "").replace("_OPEN", "").replace("_CLOSED", "").replace("_", " ")
        }
        
        return when (type) {
            ComponentType.EMPTY -> if (lang == AppLanguage.RU) "Пусто" else "Порожньо"
            ComponentType.PAN -> if (lang == AppLanguage.RU) "Камера" else "Камера"
            ComponentType.ROTATE -> if (lang == AppLanguage.RU) "Поворот" else "Повернути"
            ComponentType.INSPECT -> if (lang == AppLanguage.RU) "Свойства" else "Властивості"
            ComponentType.MULTIMETER -> if (lang == AppLanguage.RU) "Мультиметр" else "Мультиметр"
            ComponentType.BATTERY -> if (lang == AppLanguage.RU) "Батарея 9V" else "Батарея 9V"
            ComponentType.BATTERY_PACK -> if (lang == AppLanguage.RU) "Блок батарей" else "Блок батарей"
            ComponentType.COIN_CELL -> if (lang == AppLanguage.RU) "Батарейка 3V" else "Батарейка 3V"
            ComponentType.GENERATOR -> if (lang == AppLanguage.RU) "Генератор" else "Генератор"
            ComponentType.SOLAR_PANEL -> if (lang == AppLanguage.RU) "Солн. панель" else "Сонячна панель"
            ComponentType.AC_SOURCE -> if (lang == AppLanguage.RU) "Ист. пер. тока" else "Джерело зм. струму"
            ComponentType.WIND_TURBINE -> if (lang == AppLanguage.RU) "Ветряк" else "Вітряк"
            ComponentType.NUCLEAR_REACTOR -> if (lang == AppLanguage.RU) "Ядерный реактор" else "Ядерний реактор"
            ComponentType.GEOTHERMAL_GENERATOR -> if (lang == AppLanguage.RU) "Геотерм. станция" else "Геотерм. станція"
            ComponentType.HYDRO_GENERATOR -> if (lang == AppLanguage.RU) "Гидротурбина" else "Гідротурбіна"
            ComponentType.THERMOELECTRIC_GENERATOR -> if (lang == AppLanguage.RU) "Термоэлемент" else "Термоелемент"
            ComponentType.WIRE_ANY -> if (lang == AppLanguage.RU) "Провод" else "Провід"
            ComponentType.RESISTOR -> if (lang == AppLanguage.RU) "Резистор" else "Резистор"
            ComponentType.CAPACITOR -> if (lang == AppLanguage.RU) "Конденсатор" else "Конденсатор"
            ComponentType.INDUCTOR -> if (lang == AppLanguage.RU) "Катушка индук." else "Котушка індук."
            ComponentType.DIODE -> if (lang == AppLanguage.RU) "Диод" else "Діод"
            ComponentType.ZENER_DIODE -> if (lang == AppLanguage.RU) "Стабилитрон" else "Стабілітрон"
            ComponentType.FUSE -> if (lang == AppLanguage.RU) "Предохранитель" else "Запобіжник"
            ComponentType.TRANSFORMER -> if (lang == AppLanguage.RU) "Трансформатор" else "Трансформатор"
            ComponentType.SUPERCONDUCTOR -> if (lang == AppLanguage.RU) "Сверхпроводник" else "Надпровідник"
            ComponentType.HIGH_VOLTAGE_CABLE -> if (lang == AppLanguage.RU) "Высоков. кабель" else "Високов. кабель"
            ComponentType.FIBER_OPTIC -> if (lang == AppLanguage.RU) "Оптоволокно" else "Оптоволокно"
            ComponentType.SWITCH_OPEN, ComponentType.SWITCH_CLOSED -> if (lang == AppLanguage.RU) "Выключатель" else "Вимикач"
            ComponentType.PUSH_BUTTON -> if (lang == AppLanguage.RU) "Кнопка" else "Кнопка"
            ComponentType.DIP_SWITCH -> if (lang == AppLanguage.RU) "DIP-переключ." else "DIP-перемикач"
            ComponentType.REED_SWITCH -> if (lang == AppLanguage.RU) "Геркон" else "Геркон"
            ComponentType.LIMIT_SWITCH -> if (lang == AppLanguage.RU) "Концевик" else "Кінцевик"
            ComponentType.RELAY -> if (lang == AppLanguage.RU) "Электромагн. реле" else "Реле"
            ComponentType.TRANSISTOR -> if (lang == AppLanguage.RU) "Транзистор NPN" else "Транзистор NPN"
            ComponentType.MOSFET -> if (lang == AppLanguage.RU) "Полевой транз." else "Польовий транз."
            ComponentType.POTENTIOMETER -> if (lang == AppLanguage.RU) "Потенциометр" else "Потенціометр"
            ComponentType.MAGNETIC_CONTACT -> if (lang == AppLanguage.RU) "Магн. контакт" else "Магн. контакт"
            ComponentType.PRESSURE_PAD -> if (lang == AppLanguage.RU) "Сенсор давлений" else "Сенсор тиску"
            ComponentType.PHOTORESISTOR -> if (lang == AppLanguage.RU) "Фоторезистор" else "Фоторезистор"
            ComponentType.THERMISTOR -> if (lang == AppLanguage.RU) "Термистор" else "Термістор"
            ComponentType.TEMPERATURE_SENSOR -> if (lang == AppLanguage.RU) "Датчик темп." else "Датчик темп."
            ComponentType.LIGHT_SENSOR -> if (lang == AppLanguage.RU) "Датчик света" else "Датчик світла"
            ComponentType.PROXIMITY_SENSOR -> if (lang == AppLanguage.RU) "Датчик приближ." else "Датчик наближ."
            ComponentType.ULTRASONIC_SENSOR -> if (lang == AppLanguage.RU) "Ультразвук" else "Ультразвук"
            ComponentType.SOUND_SENSOR -> if (lang == AppLanguage.RU) "Датчик звука" else "Датчик звуку"
            ComponentType.VIBRATION_SENSOR -> if (lang == AppLanguage.RU) "Датчик вибр." else "Датчик вібр."
            ComponentType.GAS_SENSOR -> if (lang == AppLanguage.RU) "Датчик газа" else "Датчик газу"
            ComponentType.MOISTURE_SENSOR -> if (lang == AppLanguage.RU) "Датчик влаги" else "Датчик вологи"
            ComponentType.HALL_EFFECT_SENSOR -> if (lang == AppLanguage.RU) "Датчик Холла" else "Датчик Холла"
            ComponentType.PIR_MOTION_SENSOR -> if (lang == AppLanguage.RU) "Датчик движ." else "Датчик руху"
            ComponentType.BULB -> if (lang == AppLanguage.RU) "Лампа" else "Лампа"
            ComponentType.LED -> if (lang == AppLanguage.RU) "Светодиод" else "Світлодіод"
            ComponentType.RGB_LED -> if (lang == AppLanguage.RU) "RGB-диод" else "RGB-діод"
            ComponentType.SEVEN_SEGMENT -> if (lang == AppLanguage.RU) "7-сегм. инд." else "7-сегм. інд."
            ComponentType.FOURTEEN_SEGMENT -> if (lang == AppLanguage.RU) "14-сегм. инд." else "14-сегм. інд."
            ComponentType.LCD_DISPLAY_16X2 -> if (lang == AppLanguage.RU) "Экран LCD 16x2" else "Екран LCD 16x2"
            ComponentType.MOTOR -> if (lang == AppLanguage.RU) "Мотор" else "Мотор"
            ComponentType.SERVO_MOTOR -> if (lang == AppLanguage.RU) "Сервопривод" else "Сервопривід"
            ComponentType.STEPPER_MOTOR -> if (lang == AppLanguage.RU) "Шаговый мотор" else "Кроковий мотор"
            ComponentType.SPEAKER -> if (lang == AppLanguage.RU) "Динамик" else "Динамік"
            ComponentType.BUZZER -> if (lang == AppLanguage.RU) "Пьезопищалка" else "П'єзопищалка"
            ComponentType.LASER_DIODE -> if (lang == AppLanguage.RU) "Лазер" else "Лазер"
            ComponentType.HEATER -> if (lang == AppLanguage.RU) "Нагреватель" else "Нагрівач"
            ComponentType.COOLER -> if (lang == AppLanguage.RU) "Охладитель" else "Охолоджувач"
            ComponentType.WATER_PUMP -> if (lang == AppLanguage.RU) "Помпа" else "Помпа"
            ComponentType.FAN -> if (lang == AppLanguage.RU) "Вентилятор" else "Вентилятор"
            ComponentType.MONITOR_OLED -> if (lang == AppLanguage.RU) "Монитор OLED" else "Монітор OLED"
            ComponentType.CRT_MONITOR -> if (lang == AppLanguage.RU) "ЭЛТ Монитор" else "ЕЛТ Монітор"
            ComponentType.DISPLAY_PIXEL -> if (lang == AppLanguage.RU) "Пиксель" else "Піксель"
            ComponentType.LOGIC_AND -> if (lang == AppLanguage.RU) "И" else "І"
            ComponentType.LOGIC_OR -> if (lang == AppLanguage.RU) "ИЛИ" else "АБО"
            ComponentType.LOGIC_NOT -> if (lang == AppLanguage.RU) "НЕ" else "НЕ"
            ComponentType.LOGIC_NAND -> if (lang == AppLanguage.RU) "И-НЕ" else "І-НЕ"
            ComponentType.LOGIC_NOR -> if (lang == AppLanguage.RU) "ИЛИ-НЕ" else "АБО-НЕ"
            ComponentType.LOGIC_XOR -> if (lang == AppLanguage.RU) "Искл. ИЛИ" else "Викл. АБО"
            ComponentType.LOGIC_XNOR -> if (lang == AppLanguage.RU) "Искл. ИЛИ-НЕ" else "Викл. АБО-НЕ"
            ComponentType.PULSE_GENERATOR -> if (lang == AppLanguage.RU) "Ген. имп." else "Ген. імп."
            ComponentType.D_FLIP_FLOP -> if (lang == AppLanguage.RU) "D-триггер" else "D-тригер"
            ComponentType.T_FLIP_FLOP -> if (lang == AppLanguage.RU) "T-триггер" else "T-тригер"
            ComponentType.JK_FLIP_FLOP -> if (lang == AppLanguage.RU) "JK-триггер" else "JK-тригер"
            ComponentType.MULTIPLEXER -> if (lang == AppLanguage.RU) "Мультиплексор" else "Мультиплексор"
            ComponentType.DEMULTIPLEXER -> if (lang == AppLanguage.RU) "Демультиплексор" else "Демультиплексор"
            ComponentType.SHIFT_REGISTER -> if (lang == AppLanguage.RU) "Сдвиг. регистр" else "Зсувний регістр"
            ComponentType.HALF_ADDER -> if (lang == AppLanguage.RU) "Полусумматор" else "Напівсуматор"
            ComponentType.FULL_ADDER -> if (lang == AppLanguage.RU) "Сумматор" else "Суматор"
            ComponentType.LATCH_SR -> if (lang == AppLanguage.RU) "SR-защелка" else "SR-засувка"
            ComponentType.TIMER_555 -> if (lang == AppLanguage.RU) "Таймер 555" else "Таймер 555"
            ComponentType.OP_AMP -> if (lang == AppLanguage.RU) "Опер. усилитель" else "Опер. підсилювач"
            ComponentType.ADC -> if (lang == AppLanguage.RU) "АЦП" else "АЦП"
            ComponentType.DAC -> if (lang == AppLanguage.RU) "ЦАП" else "ЦАП"
            ComponentType.COMPARATOR -> if (lang == AppLanguage.RU) "Компаратор" else "Компаратор"
            ComponentType.VOLTAGE_REGULATOR -> if (lang == AppLanguage.RU) "Стабил. напр." else "Стабіл. напруги"
            ComponentType.AMPLIFIER -> if (lang == AppLanguage.RU) "Усилитель" else "Підсилювач"
            ComponentType.BUFFER -> if (lang == AppLanguage.RU) "Буфер" else "Буфер"
            ComponentType.MICROCONTROLLER -> if (lang == AppLanguage.RU) "МК программируемый" else "МК програмований"
            ComponentType.MEMORY_RAM -> if (lang == AppLanguage.RU) "ОЗУ RAM" else "ОЗУ RAM"
            ComponentType.MEMORY_ROM -> if (lang == AppLanguage.RU) "ПЗУ ROM" else "ПЗУ ROM"
            ComponentType.WATER -> if (lang == AppLanguage.RU) "Вода" else "Вода"
            ComponentType.LAVA -> if (lang == AppLanguage.RU) "Лава" else "Лава"
            ComponentType.OIL -> if (lang == AppLanguage.RU) "Нефть" else "Нафта"
            ComponentType.ACID -> if (lang == AppLanguage.RU) "Кислота" else "Кислота"
            ComponentType.SAND -> if (lang == AppLanguage.RU) "Песок" else "Пісок"
            ComponentType.DIRT -> if (lang == AppLanguage.RU) "Земля" else "Земля"
            ComponentType.STONE -> if (lang == AppLanguage.RU) "Камень" else "Камінь"
            ComponentType.GLASS -> if (lang == AppLanguage.RU) "Стекло" else "Скло"
            ComponentType.WOOD -> if (lang == AppLanguage.RU) "Дерево" else "Дерево"
            ComponentType.FIRE -> if (lang == AppLanguage.RU) "Огонь" else "Вогонь"
            ComponentType.ICE -> if (lang == AppLanguage.RU) "Лед" else "Лід"
            ComponentType.STEAM -> if (lang == AppLanguage.RU) "Пар" else "Пара"
            ComponentType.HELIUM -> if (lang == AppLanguage.RU) "Гелий 🎈" else "Гелій 🎈"
            ComponentType.HYDROGEN -> if (lang == AppLanguage.RU) "Водород 🔥" else "Водень 🔥"
            ComponentType.METHANE -> if (lang == AppLanguage.RU) "Метан 💨" else "Метан 💨"
            ComponentType.CARBON_DIOXIDE -> if (lang == AppLanguage.RU) "Углекислый газ" else "Вуглекислий газ"
            ComponentType.SLIME -> if (lang == AppLanguage.RU) "Слизь" else "Слиз"
            ComponentType.RUBBER -> if (lang == AppLanguage.RU) "Резина" else "Гума"
            ComponentType.DIAMOND -> if (lang == AppLanguage.RU) "Алмаз" else "Алмаз"
            ComponentType.COAL -> if (lang == AppLanguage.RU) "Уголь" else "Вугілля"
            ComponentType.SPONGE -> if (lang == AppLanguage.RU) "Губка" else "Губка"
            ComponentType.GASOLINE -> if (lang == AppLanguage.RU) "Бензин" else "Бензин"
            ComponentType.LIQUID_NITROGEN -> if (lang == AppLanguage.RU) "Жидк. азот" else "Рідк. азот"
            ComponentType.URANIUM -> if (lang == AppLanguage.RU) "Уран" else "Уран"
            ComponentType.MAGIC_DUST -> if (lang == AppLanguage.RU) "Пыль" else "Пил"
            ComponentType.PIPE -> if (lang == AppLanguage.RU) "Труба гидравл." else "Труба гідравл."
            ComponentType.INFINITE_BATTERY -> if (lang == AppLanguage.RU) "Бескон. батар." else "Нескінченна батар."
            ComponentType.INFINITE_WATER -> if (lang == AppLanguage.RU) "Родник воды" else "Джерело води"
            ComponentType.INFINITE_LAVA -> if (lang == AppLanguage.RU) "Источник лавы" else "Джерело лави"
            ComponentType.INFINITE_OIL -> if (lang == AppLanguage.RU) "Источник масла" else "Джерело мастила"
            ComponentType.INFINITE_ACID -> if (lang == AppLanguage.RU) "Источник кислоты" else "Джерело кислоти"
            ComponentType.INFINITE_SLIME -> if (lang == AppLanguage.RU) "Источник слизи" else "Джерело слизу"
            ComponentType.INFINITE_GASOLINE -> if (lang == AppLanguage.RU) "Источник бензина" else "Джерело бензину"
            ComponentType.INFINITE_LIQUID_NITROGEN -> if (lang == AppLanguage.RU) "Источник азота" else "Джерело азоту"
            ComponentType.INFINITE_STEAM -> if (lang == AppLanguage.RU) "Источник пара" else "Джерело пари"
            ComponentType.FLUID_DRAIN -> if (lang == AppLanguage.RU) "Слив" else "Злив"
            ComponentType.VOID_HOLE -> if (lang == AppLanguage.RU) "Пустота" else "Порожнеча"
            ComponentType.CONVEYOR_BELT -> if (lang == AppLanguage.RU) "Конвейер" else "Конвеєр"
            ComponentType.MAGNET -> if (lang == AppLanguage.RU) "Магнит" else "Магніт"
            ComponentType.PISTON -> if (lang == AppLanguage.RU) "Поршень" else "Поршень"
            ComponentType.DOUBLE_DOOR -> if (lang == AppLanguage.RU) "Двустор. Дверь" else "Двосторонні Двері"
            ComponentType.KEYPAD_4X4 -> if (lang == AppLanguage.RU) "Клавиатура 4x4" else "Клавіатура 4x4"
            ComponentType.JOYSTICK -> if (lang == AppLanguage.RU) "Джойстик" else "Джойстик"
            ComponentType.ACCELEROMETER -> if (lang == AppLanguage.RU) "Акселерометр" else "Акселерометр"
            ComponentType.COLOR_SENSOR -> if (lang == AppLanguage.RU) "Сенсор цвета" else "Сенсор кольору"
            ComponentType.STEEL -> if (lang == AppLanguage.RU) "Сталь" else "Сталь"
            ComponentType.COPPER -> if (lang == AppLanguage.RU) "Медь" else "Мідь"
            ComponentType.GOLD -> if (lang == AppLanguage.RU) "Золото" else "Золото"
            ComponentType.PLASTIC -> if (lang == AppLanguage.RU) "Пластик" else "Пластик"
            ComponentType.PLASMA -> if (lang == AppLanguage.RU) "Плазма" else if (lang == AppLanguage.UK) "Плазма" else "Plasma"
            ComponentType.INFINITE_PLASMA -> if (lang == AppLanguage.RU) "Источник плазмы" else if (lang == AppLanguage.UK) "Джерело плазми" else "Infinite Plasma"
            ComponentType.BLACK_HOLE -> if (lang == AppLanguage.RU) "Черная дыра" else if (lang == AppLanguage.UK) "Чорна діра" else "Black Hole"
            ComponentType.PORTAL_IN -> if (lang == AppLanguage.RU) "Портал (Вход)" else if (lang == AppLanguage.UK) "Портал (Вхід)" else "Portal (In)"
            ComponentType.PORTAL_OUT -> if (lang == AppLanguage.RU) "Портал (Выход)" else if (lang == AppLanguage.UK) "Портал (Вихід)" else "Portal (Out)"
            ComponentType.TESLA_COIL -> if (lang == AppLanguage.RU) "Катушка Тесла" else if (lang == AppLanguage.UK) "Котушка Тесла" else "Tesla Coil"
            ComponentType.MERCURY -> if (lang == AppLanguage.RU) "Ртуть" else if (lang == AppLanguage.UK) "Ртуть" else "Mercury"
            ComponentType.LIGHTNING_ROD -> if (lang == AppLanguage.RU) "Громоотвод" else if (lang == AppLanguage.UK) "Блискавковідвід" else "Lightning Rod"
            ComponentType.STIRLING_ENGINE -> if (lang == AppLanguage.RU) "Двигатель Стирлинга" else if (lang == AppLanguage.UK) "Двигун Стірлінга" else "Stirling Engine"
            ComponentType.QUANTUM_SUPERCONDUCTOR -> if (lang == AppLanguage.RU) "Квантовый Сверхпроводник" else if (lang == AppLanguage.UK) "Квантовий Надпровідник" else "Quantum Superconductor"
            ComponentType.PCM_CELL -> if (lang == AppLanguage.RU) "ФАЗ-Элемент" else if (lang == AppLanguage.UK) "ФАЗ-Елемент" else "PCM Thermal Cell"
            ComponentType.LASER_RECEIVER -> if (lang == AppLanguage.RU) "Лазерный Приемник" else if (lang == AppLanguage.UK) "Лазерний Приймач" else "Laser Receiver"
            ComponentType.GRAPHITE_ROD -> if (lang == AppLanguage.RU) "Графитовый Стержень" else if (lang == AppLanguage.UK) "Графітовий Стрижень" else "Graphite Rod"
            ComponentType.PIEZO_SENSOR -> if (lang == AppLanguage.RU) "Пьезодатчик" else if (lang == AppLanguage.UK) "П'єзодатчик" else "Piezo Sensor"
            else -> type.name.replace("_ANY", "").replace("_OPEN", "").replace("_CLOSED", "").replace("_", " ")
        }
    }
}
