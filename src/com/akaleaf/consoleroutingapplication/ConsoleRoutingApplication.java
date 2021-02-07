package com.akaleaf.consoleroutingapplication;

import java.io.File;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 *
 * @author akaleaf
 */
public class ConsoleRoutingApplication {

    public static final String MSG_DELIM = "==========================================";
    public static final String MSG_COMMAND_NOT_FOUND = "Ошибка: Команда не найдена";
    public static final String MSG_NOT_ENOUGH_ARGUMENTS = "Ошибка: Недостаточно аргументов для выполнения команды";
    public static final String MSG_INVALID_DATA_STRUCTURE = "Ошибка: Неверная структура данных";
    public static final String MSG_INVALID_OPERAND = "Ошибка: Некорректный операнд: Операнд не принадлежит к типу регистра или памяти";
    public static final String MSG_UNKNOWN_ERROR = "Ошибка: Неизвестная ошибка";
    
    private Map<String, Command> commands;

    private String consoleEncoding;

    // ?: '/'(Linux, MacOS) OR '\'(Windows)
    private char slash;

    public ConsoleRoutingApplication(String consoleEncoding) {
        commands = new TreeMap<>();
        Command
        cmd = new HelpCommand();
        commands.put(cmd.getName(), cmd);
        cmd = new DirCommand();
        commands.put(cmd.getName(), cmd);
        cmd = new ExitCommand();
        commands.put(cmd.getName(), cmd);
        cmd = new CdCommand();
        commands.put(cmd.getName(), cmd);
        cmd = new CnCommand();
        commands.put(cmd.getName(), cmd);
        cmd = new RmCommand();
        commands.put(cmd.getName(), cmd);
        cmd = (Command) new AsmCommand();
        commands.put(cmd.getName(), cmd);
        this.consoleEncoding = consoleEncoding;
    }

    public String toHex(String arg) {
        return String.format("%040x", new BigInteger(1, arg.getBytes(/*YOUR_CHARSET?*/)));
    }
    
    public void execute() {
        
        if (System.getProperty("os.name").equals("Windows 10")) {
            slash = '\\';
        } else {
            slash = '/';
        }
        
        Context context = new Context();

        context.currentDirectory = new File(".").getAbsoluteFile();
        
        // Убираем "/." - начало
        String substring = context.currentDirectory + "";
        substring = substring.substring(0, substring.length() - 2);
        File fileSubstring = new File(substring);
        context.setNewCurrentDirectory(fileSubstring);
        // Убираем "/." - конец

        boolean result = true;
        Scanner scanner = new Scanner(System.in, consoleEncoding);
        do {
            System.out.print(context.currentDirectory + "> ");
            String fullCommand = scanner.nextLine();
            ParsedCommand parsedCommand = new ParsedCommand(fullCommand);
            if (parsedCommand.command == null || "".equals(parsedCommand.command)) {
                continue;
            }
            Command command = commands.get(parsedCommand.command.toUpperCase());
            if (command == null) {
                System.out.println(MSG_COMMAND_NOT_FOUND);
                continue;
            }
            result = command.execute(context, parsedCommand.args);
        } while (result);
    }

    public static void main(String[] args) {
        ConsoleRoutingApplication cp = new ConsoleRoutingApplication("Cp1251");
        cp.execute();
    }


    class ParsedCommand {

        String command;

        String[] args;

        public ParsedCommand(String line) {
            String parts[] = line.split(" ");
            if (parts != null) {
                command = parts[0];
                if (parts.length > 1) {
                    args = new String[parts.length - 1];
                    System.arraycopy(parts, 1, args, 0, args.length);
                }
            }
        }
    }

    interface Command {

        boolean execute(Context context, String... args);

        void printDescription();

        String getName();

        String getDescription();
    }

    class Context {

        private File currentDirectory;

        public void setNewCurrentDirectory(File newDirectory) {
            currentDirectory = newDirectory;
        };
        
        public File getCurrentDirectory() {
            return currentDirectory;
        }

    }
//
//    Dat1 db 1, 2, 10, 15
//    Dat2 dw 1234
//    Dat3 db dup 100(0)
//    Dat4 db 'Hello!'
//    
    class AsmCommand implements Command {
    
        // Метод для преобразования строки в массив символов, а далее - в коды этих символов согласно ASCII-таблице
        String stringToHex(String string) {
            char charsFromString[] = string.toCharArray();
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < charsFromString.length; i++) {
                String hexString = Integer.toHexString(charsFromString[i]);
                stringBuffer.append(hexString);
            }
            return stringBuffer.toString();
        }
        
        boolean isNumeric(String string) {
            try {
                Integer.parseInt(string);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // Метод для преобразования какого-либо значения в Hex представление этого значения.
        // Для строки метод будет использовать значения из ASCII
        // Для "?" метод возвращает "00"
        // Для числа метод будет использовать обычное преобразование
        String toHex(String value) {
            switch (value.charAt(0)) {
                case '\'':
                    value = value.replaceAll("[',]", "");
                    return stringToHex(value);
                case '?':
                    return "00";
                default:
                    value = value.replaceAll("[,]", "");
                    String hexValue = Integer.toHexString(Integer.parseInt(value));
                    if ((hexValue.length() % 2) == 1) {
                        hexValue = "0" + hexValue;
                    }
                    return hexValue;
            }
        }
        
        String[] removeEmptyValuesFromTheArray(String[] array) {
            // Убрать пустые значения из массива строк
            return Arrays.stream(array).filter(value ->
                    value != null && value.length() > 0
            )
            .toArray(size -> new String[size]);
        }
        
        // Метод для вывода ТСИ в консольку
        void printSNT(List<String[]> snt) {
            // Вывод ТСИ в консольку - начало
            System.out.println(MSG_DELIM);

            // Для более красивого вывода определим максимальную длину среди наименований переменных
            int maxLengthAmongNames = 0;
            for (String[] sntArray: snt) {
                for (int i = 0; i < 3; i++) {
                    if (maxLengthAmongNames < sntArray[0].length()) {
                        maxLengthAmongNames = sntArray[0].length();
                    }
                }
            }

            System.out.format("%" + maxLengthAmongNames + "s", "Name");
            System.out.println("   CA Byte Hex");
            for (String[] sntArray: snt) {
                for (int i = 0; i < 4; i++) {
                    switch (i) {
                        case 0:
                            System.out.format("%-" + maxLengthAmongNames + "s", sntArray[i]);
                            break;
                        case 1:
                            int increm = 4;
                            while (increm > sntArray[i].length()) {
                                System.out.print("0");
                                increm--;
                            }
                            System.out.print(sntArray[i]);
                            break;
                        case 2:
                            System.out.format("%4s", sntArray[i]);
                            break;
                        case 3:
                            System.out.print(sntArray[i]);
                        default:
                            break;
                    }
                    System.out.print(" ");
                }
                System.out.println();
            }
            System.out.println(MSG_DELIM);
            // Вывод ТСИ в консольку - конец
        }
        
        @Override
        public boolean execute(Context context, String ... args) {
            // Если команда asm выполнена без ввода аргумента
            if (args == null) {
                // Вывести сообщение о недостаточности аргументов
                System.out.println(MSG_NOT_ENOUGH_ARGUMENTS);
            } else {
                try {
                    // Создаём экземпляр файла по указанному пути
                    FileReader file = new FileReader(context.getCurrentDirectory() + Character.toString(slash) + args[0]);
                    Scanner scannedFile = new Scanner(file);
                    
                    // CA или AC - Счётчик Адреса - Address Counter
                    int addressCounter = 0;
                    int addressCounterCodeSegment = 0;
                    
                    // SNT stands for Symbolic Name Table. ТСИ - Таблица Символических Имён.
                    List<String[]> snt = new ArrayList<>();
                    
                    // Hex представление сегмента ".CODE"
                    List<String[]> hexCodesOfTheCodeSegment = new ArrayList<>();
                    
                    // Определение сегмента файла
                    // Принимает значения ".DATA", ".CODE"
                    String segment = "";
                    
                    // Инициализация статичной таблицы команд - начало
                    // Название | Адрес | Байты | Операнд 1 | Операнд 2
                    final String[][] commandTable = {
                    {"MOV", "01", "4", "REGISTER", "MEMORY"},
                    {"MOV", "02", "4", "MEMORY", "REGISTER"},
                    {"MOV", "03", "3", "REGISTER", "REGISTER"},
                    {"MOV", "04", "3", "REGISTER", "CONSTANT1B"},
                    {"MOV", "05", "4", "REGISTER", "CONSTANT2B"},
                    {"ADD", "06", "3", "REGISTER", "REGISTER"},
                    {"ADD", "07", "3", "REGISTER", "CONSTANT1B"},
                    {"ADD", "08", "4", "REGISTER", "CONSTANT2B"},
                    {"SUB", "09", "3", "REGISTER", "REGISTER"},
                    {"SUB", "0A", "3", "REGISTER", "CONSTANT1B"},
                    {"SUB", "0B", "4", "REGISTER", "CONSTANT2B"},
                    {"INC", "0C", "2", "REGISTER", ""},
                    {"DEC", "0D", "2", "REGISTER", ""},
                    {"MUL", "0E", "2", "REGISTER", ""},
                    };
                    // Инициализация статичной таблицы команд - конец

                    // Инициализация статичной таблицы регистров - начало
                    final String[][] registerTable = {
                    {"AL", "01"},
                    {"AH", "02"},
                    {"AX", "03"},
                    {"BL", "04"},
                    {"BH", "05"},
                    {"BX", "06"},
                    {"CL", "07"},
                    {"CH", "08"},
                    {"CX", "09"},
                    {"DL", "0A"},
                    {"DH", "0B"},
                    {"DX", "0C"},
                    };
                    // Инициализация статичной таблицы регистров - конец
                    
                    // Проходим по каждой строке отсканированного файла
                    while (scannedFile.hasNextLine()) {
                        
                        // Разделить строку на массив слов. Разделителем выступает " "
                        String splittedLine[] = scannedFile.nextLine().split(" ");
                        
                        // Уберём пустые элементы из массива
                        splittedLine = removeEmptyValuesFromTheArray(splittedLine);
 
                        // Определяем на каком сегменте мы находимся на момент обработки строки
                        if (splittedLine[0].equals(".DATA")) {
                            segment = ".DATA";
                            continue;
                        } else if (splittedLine[0].equals(".CODE")) {
                            segment = ".CODE";
                            continue;
                        }
                        
                        // СЕГМЕНТ 
                        // ОБРАБОТКИ 
                        // .DATA
                        if (segment.equals(".DATA")) {
                            
                            // Специализированный массив, который в последствии будет добавлен в список массивов
                            // Элементы массива представляют собой:
                            // Имя переменной, CA, Байтовый размер, Hex формат (не Нёх)
                            String[] sntArrayToAdd = new String[4];

                            // Чтобы в sntArrayToAdd не было null
                            // Пояснение:
                            // случай, если sntArrayToAdd[3] == null:
                            // sntArrayToAdd[3] += "hello" //ВЫВОД// sntArrayToAdd[3] == "nullhello"
                            // случай, если sntArrayToAdd[3] == "":
                            // sntArrayToAdd[3] += "hello" //ВЫВОД// sntArrayToAdd[3] == "hello"
                            sntArrayToAdd[3] = "";

                            // Обработка имени переменной - начало
                            // В общих чертах: пропускаем строку, если переменная с таким названием уже есть в списке
                            boolean alreadyHaveThisName = false;
                            for (String[] sntItem: snt) {
                                if (sntItem[0].equals(splittedLine[0])) {
                                    alreadyHaveThisName = true;
                                }
                            }
                            // Пропускаем строку
                            if (alreadyHaveThisName) continue;
                            // А если не пропускаем строку, значит новое имя заносим в массив
                            sntArrayToAdd[0] = splittedLine[0];
                            // Обработка имени переменной - конец

                            // Добавим в массив AC в Hex формате
                            sntArrayToAdd[1] = Integer.toHexString(addressCounter);

                            // Для определения байтового размера. Для db = 1, dw = 2, ...
                            int byteSizeFlag;

                            // Определение байтового размера на основе команды - начало
                            switch (splittedLine[1]) {
                                case "db":
                                    byteSizeFlag = 1;
                                    break;
                                case "dw":
                                    byteSizeFlag = 2;
                                    break;
                                case "dd":
                                    byteSizeFlag = 4;
                                    break;
                                case "dq":
                                    byteSizeFlag = 8;
                                    break;
                                default:
                                    // Если в строке определено какое-то иное слово,
                                    // то выведем сообщение о ошибке
                                    System.out.println(MSG_INVALID_DATA_STRUCTURE);
                                    //  и остановим выполнение приложения
                                    return false;
                            }
                            // Определение байтового размера на основе команды - конец

                            // Добавим в массив значение байтового размера
                            sntArrayToAdd[2] = Integer.toString(byteSizeFlag);

                            // Если третье слово в строке это "dup"
                            if (splittedLine[2].equals("dup")) {
                                // После "dup" стоит какое-либо число. Например 100 в строке "dup 100(0)"
                                // "dups" будет хранить это число
                                String dups = "";
                                // Определяем какое число стоит после "dup"
                                int incr = 0;
                                while (!(splittedLine[3].charAt(incr) == '(')) {
                                    dups += splittedLine[3].charAt(incr++);
                                }
                                incr++;
                                // В скобках после "dup" стоит какое-либо значение. Например 0 в строке "dup 100(0)"
                                // "number" будет хранить это значение
                                String value = "";
                                while (!(splittedLine[3].charAt(incr) == ')')) {
                                    value += splittedLine[3].charAt(incr++);
                                }
                                // dups раз переводим nubmer в Hex-представление и dups раз добавляем number в массив
                                for (int increm = 0; increm < Integer.parseInt(dups); increm++) {
                                    sntArrayToAdd[3] += toHex(value);
                                }
                                // Подсчитываем CA
                                addressCounter += (byteSizeFlag * Integer.parseInt(dups));
                            } 
                            // Если третье слово в строке это не "dup"
                            else {
                                // Проходим по всем значениям, переводим их в Hex представление и заносим в массив
                                // ДОДЕЛАТЬ: 
                                // рассмотреть случай "Dat1 db 1, 2, 10, 15, , ,"
                                int increment = 2;
                                while (increment < splittedLine.length) {
                                    sntArrayToAdd[3] += (toHex(splittedLine[increment])).toUpperCase();
                                    increment++;
                                }
                                // Подсчитываем CA
                                addressCounter += (byteSizeFlag * (splittedLine.length - 2));
                            }
                            // Добавляем в список массив
                            snt.add(sntArrayToAdd);
                        } else 
                            // СЕГМЕНТ 
                            // ОБРАБОТКИ 
                            // .CODE
                        if (segment.equals(".CODE")) {
                            
                            // Уберём из массива строки лишние символы
                            for (int i = 0; i < splittedLine.length; i++){
                                splittedLine[i] = splittedLine[i].replaceAll("[,	]", "");
                            }
                            
                            String command = splittedLine[0];
                            String[] hexCodeOfTheLine = new String[3];
                            
                            // Проверяем есть ли команда в таблице - начало
                            boolean commandIsCorrect = false;
                            
                            // commandIndex - индекс в таблице, на котором нашли команду. Её сохраним на будущее
                            int commandIndex = 0;
                            
                            for (String commandTableItem[] : commandTable) {
                                // Если команда в таблице есть
                                if (commandTableItem[0].equals(command.toUpperCase())) {
                                    commandIsCorrect = true;
                                    break;
                                }
                                commandIndex++;
                            }
                            
                            // Если команды в таблице нет
                            if (!commandIsCorrect) {
                                // Вывести сообщение об ошибке
                                System.out.println(MSG_COMMAND_NOT_FOUND + ": " + command);
                                // И завершить работу приложения
                                return false;
                            }
                            // Проверяем есть ли команда в таблице - конец
                            
                            // Проверяем какого типа первый операнд:
                            // Регистр или память? - начало
                            String firstOperand = splittedLine[1];
                            String firstOperandHex = "";
                            String firstOperandType = "";
                            
                            // Пройдёмся по таблице регистров
                            for (String registerTableItem[] : registerTable) {
                                // Если имя операнда совпало с каким-либо именем из таблицы регистров
                                if (registerTableItem[0].equals(firstOperand.toUpperCase())) {
                                    // Значит теперь наш операнд имеет тип - REGISTER
                                    firstOperandType = "REGISTER";
                                    // Заодно заберём его код, чтобы ещё раз не приходить
                                    firstOperandHex = registerTableItem[1];
                                    break;
                                }
                            }
                            
                            // Если тип операнда не определился после предыдущего цикла
                            if (firstOperandType.equals("")) {
                                // Пройдёмся по ТСИ
                                for (String sntItem[] : snt) {
                                    // Если имя операнда совпало с каким-либо именем из ТСИ
                                    if (sntItem[0].equals(firstOperand)) {
                                        // Значит теперь наш операнд имеет тип - MEMORY
                                        firstOperandType = "MEMORY";
                                        // У операнда оказался маленький код
                                        // Попросим его не расстраиваться
                                        // И предложим свою помощь:
                                        int increm = 4;
                                        String zeros = "";
                                        while (increm > firstOperand.length()) {
                                            zeros += "0";
                                            increm--;
                                        }
                                        // Теперь все равны
                                        firstOperandHex = zeros + sntItem[1];
                                        break;
                                    }
                                }
                            }
                            
                            // Если операнд оказался не памятью и не регистром
                            if (firstOperandType.equals("")) {
                                // Вывести сообщение об ошибке
                                System.out.println(MSG_INVALID_OPERAND + ": " + firstOperand);
                                // И завершить работу приложения
                                return false;
                            }
                            // Проверяем какого типа первый операнд:
                            // Регистр или память? - конец
                            
                            // Важно объявить до блока try{}
                            String secondOperandType = "";
                            String secondOperandHex = "";
                            
                            // Второго операнда может не оказаться, поэтому исопльзуем try{}
                            try {
                                // Проверяем какого типа второй операнд:
                                // Регистр, память или константа? - начало
                                String secondOperand = splittedLine[2];
                                
                                // Если в имени операнда если только цифры
                                if (isNumeric(secondOperand)) {
                                    // Если в имени операнда хранится число меньше 256 и больше -129
                                    if (Integer.parseInt(secondOperand) < 256 || Integer.parseInt(secondOperand) > -129) {
                                        // То наш операнд может стать однобайтовым
                                        secondOperandType = "CONSTANT1B";
                                    } else {
                                        // в противном случае пусть будет пока что считаться двухбайтовым
                                        secondOperandType = "CONSTANT2B";
                                    }
                                    // Заодно заберём с собой его код
                                    secondOperandHex = toHex(secondOperand);
                                }
                                
                                // Операнд не оказался константой?
                                if (secondOperandType.equals("")) {
                                    // Тогда пройдёмся по таблице регистров
                                    for (String registerTableItem[] : registerTable) {
                                        // Если есть совпадение имени операнда с каким-либо именем из таблицы регистров
                                        if (registerTableItem[0].equals(secondOperand.toUpperCase())) {
                                            // Значит теперь наш операнд имеет тип - REGISTER
                                            secondOperandType = "REGISTER";
                                            // Заберём код
                                            secondOperandHex = registerTableItem[1];
                                            break;
                                        }
                                    }
                                }
                                
                                // Всё ещё непонятно какого он типа?
                                if (secondOperandType.equals("")) {
                                    // Тогда пройдёмся по ТСИ
                                    for (String sntItem[] : snt) {
                                        // Если есть совпадение
                                        if (sntItem[0].equals(secondOperand)) {
                                            // Значит наш операнд имеет тип - MEMORY
                                            secondOperandType = "MEMORY";
                                            // Дополним код операнда нулями
                                            int increm = 4;
                                            String zeros = "";
                                            while (increm > secondOperand.length()) {
                                                zeros += "0";
                                                increm--;
                                            }
                                            secondOperandHex = zeros + sntItem[1];
                                            break;
                                        }
                                    }
                                }
                                
                                // Если операнд оказался не памятью, не регистром и не константой
                                if (secondOperandType.equals("")) {
                                    // Вывести сообщение об ошибке
                                    System.out.println(MSG_INVALID_OPERAND + ": " + secondOperand.toUpperCase());
                                    // И завершить работу приложения
                                    return false;
                                }
                                
                                // Проверяем какого типа второй операнд:
                                // Регистр, память или константа? - конец
                            } catch (Exception e) {}
                            
                            // Определение двоичного кода команды - начало
                            String binaryCode = "";
                            for (int i = commandIndex; i < commandTable.length; i++) {
                                if (commandTable[i][0].equals(command.toUpperCase())) {
                                    if (commandTable[i][3].equals(firstOperandType)) {
                                        if (commandTable[i][4].equals(secondOperandType)) {
                                            binaryCode = commandTable[i][1];
                                            // CA здесь меняется
                                            addressCounterCodeSegment += Integer.parseInt(commandTable[i][2]);
                                            // Проверим CA, вывовывоводив в консоль
                                            System.out.println("CA: " + Integer.toHexString(addressCounterCodeSegment));
                                            break;
                                        }
                                    }
                                }
                            }
                            // Определение двоичного кода команды - конец
                            
                            // Соберём результат в кучку - начало
                            hexCodeOfTheLine[0] = binaryCode;
                            hexCodeOfTheLine[1] = firstOperandHex;
                            hexCodeOfTheLine[2] = secondOperandHex;
                            // Соберём результат в кучку - конец
                            
                            // Добавим массив в основную массу
                            hexCodesOfTheCodeSegment.add(hexCodeOfTheLine);
                            
                        }
                        
                    }
                    
                    // Выведем таблицу ТСИ в консоль
                    System.out.println("  Таблица Символических Имён:");
                    printSNT(snt);
                    
                    // Выведем Hex представление сегмента ".CODE"
                    System.out.println("  HEX представление кода программы:");
                    for (int i = 0; i < hexCodesOfTheCodeSegment.size(); i++) {
                        hexCodesOfTheCodeSegment.set(i, removeEmptyValuesFromTheArray(hexCodesOfTheCodeSegment.get(i)));
                        System.out.println(Arrays.toString(hexCodesOfTheCodeSegment.get(i)));
                    }
                    
                    // Закроем потоки - начало
                    scannedFile.close();
                    file.close();
                    // Закроем потоки - конец
                    
                } catch (Exception e) {
                    System.out.println(MSG_UNKNOWN_ERROR);
                }
            }
            return true;
        }

        @Override
        public void printDescription() {
            System.out.println(getDescription());
        }

        @Override
        public String getName() {
            return "ASM";
        }

        @Override
        public String getDescription() {
            return "Интерпретация сегмента объявления данных с Intel диалекта Ассемблера\n"
                    + "    asm (файл-источник)";
        }
    
    }
    class HelpCommand implements Command {

        @Override
        public boolean execute(Context context, String... args) {
            if (args == null) {
                System.out.println("Список доступных комманд:\n" + MSG_DELIM);
                for (Command cmd : commands.values()) {
                    System.out.println(cmd.getName() + ": " + cmd.getDescription());
                }
                System.out.println(MSG_DELIM);
            } else {
                for (String cmd : args) {
                    System.out.println("Помощь по команде " + cmd + ":\n" + MSG_DELIM);
                    Command command = commands.get(cmd.toUpperCase());
                    if (command == null) {
                        System.out.println(MSG_COMMAND_NOT_FOUND);
                    } else {
                        command.printDescription();
                    }
                    System.out.println(MSG_DELIM);
                }
            }
            return true;
        }

        @Override
        public void printDescription() {
            System.out.println(getDescription());
        }

        @Override
        public String getName() {
            return "HELP";
        }

        @Override
        public String getDescription() {
            return "Отображение списка доступных комманд";
        }
    }
    
    class CdCommand implements Command {

        @Override
        public void printDescription() {
            System.out.println(getDescription());
        }

        @Override
        public boolean execute(Context context, String... args) {
            if (args == null) {
                System.out.println(MSG_NOT_ENOUGH_ARGUMENTS);
            } else {
                // Переход в каталог args[0]
                if (args[0].equals("..")) {
                    String argDirectoryString = context.currentDirectory + "";
                    while (!(argDirectoryString.charAt(argDirectoryString.length() - 1) == slash)) {
                        argDirectoryString = argDirectoryString.substring(0, argDirectoryString.length() - 1);
                    }
                    File argDirectory = new File(argDirectoryString);
                    context.setNewCurrentDirectory(argDirectory);
                } else {
                    File argDirectory = new File(context.currentDirectory + Character.toString(slash) + args[0] + Character.toString(slash));
                    context.setNewCurrentDirectory(argDirectory);
                }

            }
            return true;
        }

        @Override
        public String getName() {
            return "CD";
        }

        @Override
        public String getDescription() {
            return "Изменение текущего каталога";
        }
    }

    class DirCommand implements Command {

        @Override
        public void printDescription() {
            System.out.println(getDescription());
        }

        @Override
        public boolean execute(Context context, String... args) {
            if (args == null) {
                // вывести содержимое текущей директории
                printDir(context.currentDirectory);
            } else {
                // вывести содержимое определённой не текущей директории
                File argDirectory = new File(context.currentDirectory + "/" + args[0] + "/");
                printDir(argDirectory);
            }
            return true;
        }

        @Override
        public String getName() {
            return "DIR";
        }

        private void printDir(File dir) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        System.out.print("+ ");
                        System.out.println(f.getName());
                    }
                }
                for (File f : files) {
                    if (!(f.isDirectory())) {
                        System.out.print("- ");
                        System.out.println(f.getName());
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "Отображение содержимого каталога";
        }
    }


    class CnCommand implements Command {
        @Override
        public boolean execute(Context context, String... args) {
            if ((args[0] == null) || (args[1] == null)) {
                System.out.println(MSG_NOT_ENOUGH_ARGUMENTS);
            } else {
                File file = new File(args[0]);
                File file2 = new File(args[1]);
                file.renameTo(file2);
            }

            return true;
        }

        @Override
        public void printDescription() {
            System.out.println(getDescription());
        }

        @Override
        public String getName() {
            return "CN";
        }

        @Override
        public String getDescription() {
            return "Изменение имени файла";
        }
    }

    class RmCommand implements Command {
        @Override
        public boolean execute(Context context, String... args) {
            if ((args[0] == null)) {
                System.out.println(MSG_NOT_ENOUGH_ARGUMENTS);
            } else {
                File file = new File(args[0]);
                file.delete();
            }

            return true;
        }

        @Override
        public void printDescription() {
            System.out.println(getDescription());
        }

        @Override
        public String getName() {
            return "RM";
        }

        @Override
        public String getDescription() {
            return "Удаление файла";
        }
    }

    class ExitCommand implements Command {
        @Override
        public boolean execute(Context context, String... args) {
            System.out.println("Завершение работы приложения... Готово.");
            return false;
        }

        @Override
        public void printDescription() {
            System.out.println(getDescription());
        }

        @Override
        public String getName() {
            return "EXIT";
        }

        @Override
        public String getDescription() {
            return "Завершение работы приложения";
        }
    }
}