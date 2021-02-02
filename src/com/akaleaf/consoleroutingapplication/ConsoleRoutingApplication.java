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
        
        @Override
        public boolean execute(Context context, String... args) {
            // Если команда asm выполнена без ввода аргумента
            if (args == null) {
                // Вывести сообщение о недостаточности аргументов
                System.out.println(MSG_NOT_ENOUGH_ARGUMENTS);
            } else {
                try {
                    FileReader file = new FileReader(args[0]);
                    Scanner scannedFile = new Scanner(file);
                    // CA или AC - Счётчик Адреса - Address Counter
                    int addressCounter = 0;
                    
                    // SNT stands for Symbolic Name Table. ТСИ - Таблица Символических Имён.
                    List<String[]> snt = new ArrayList<String[]>();
                    while (scannedFile.hasNextLine()) {
                        // Для определения байтового размера. Для db = 1, dw = 2, ...
                        int byteSizeFlag = 0;
                        // Разделить строку на массив слов. Разделителем выступает " "
                        String splittedLine[] = scannedFile.nextLine().split(" ");
                        // Специализированный массив, который в последствии будет добавлен в список массивов
                        // Элементы массива представляют собой:
                        // Имя переменной, CA, Байтовый размер, Hex формат
                        String[] sntArrayToAdd = new String[4];
                        // Чтобы в sntArrayToAdd не было null
                        sntArrayToAdd[3] = "";
                        
                        // Обработка имени переменной - начало
                        // В общих чертах: пропускаем строку, если переменная с таким названием уже объявлена
                        boolean skipScanningTheLine = false;
                        for (String[] sntArray: snt) {
                            if (sntArray[0].equals(splittedLine[0])) {
                                skipScanningTheLine = true;
                            }
                        }
                        // Пропускаем строку
                        if (skipScanningTheLine) continue;
                        // А если дошли до сюда, значит строку не пропускаем и новое имя заносим в массив
                        sntArrayToAdd[0] = splittedLine[0];
                        // Обработка имени переменной - конец
                        
                        // Добавим в массив AC в Hex формате
                        sntArrayToAdd[1] = Integer.toHexString(addressCounter);
                        
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
                                // то выведем сообщение о ошибке и остановим выполнение приложения
                                System.out.println(MSG_INVALID_DATA_STRUCTURE);
                                return false;
                        }
                        // Определение байтового размера на основе команды - конец
                        
                        // Добавим в массив значение байтового размера
                        sntArrayToAdd[2] = Integer.toString(byteSizeFlag);
                        
                        // Если третье слово в строке это "dup"
                        if (splittedLine[2].equals("dup")) {
                            // После "dup" стоит какое-либо число. Например 100 в строке "dup 100(0)"
                            String dups = "";
                            int incr = 0;
                            // Определяем какое число стоит после "dup"
                            while (!(splittedLine[3].charAt(incr) == '(')) {
                                dups += splittedLine[3].charAt(incr++);
                            }
                            incr++;
                            String number = "";
                            while (!(splittedLine[3].charAt(incr) == ')')) {
                                number += splittedLine[3].charAt(incr++);
                            }
                            for (int increm = 0; increm < Integer.parseInt(dups); increm++) {
                                sntArrayToAdd[3] += stringToHex(number);
                            }
                            System.out.println(splittedLine[3]);
                            addressCounter = addressCounter + (byteSizeFlag * Integer.parseInt(dups));
                        } else {
                            int increment = splittedLine.length;
                            while (increment > 2) {
                                splittedLine[increment - 1] = splittedLine[increment - 1].replaceAll("[, ']", "");
                                System.out.println(splittedLine[increment - 1]);
                                sntArrayToAdd[3] += stringToHex(splittedLine[increment - 1]);
                                increment--;
                            }
                            addressCounter = addressCounter + (byteSizeFlag * (splittedLine.length - 2));
                        }
                        snt.add(sntArrayToAdd);
                    }
                    
                    // ДОДЕЛАТЬ:
                    // Скорее всего придётся переписывать все значения (1, 2, 3, 4, 1234, 'Hello!') в отдельный массив,
                    // чтобы отдельно прорабатывать, т.к. в теории может попасться строка с разными типами по типу:
                    // Dat5 dw 5, 'Hello', 7
                    // Миша, всё хуйня, давай по новой
                    
                    // Вывод ТСИ в консольку - начало
                    System.out.println(MSG_DELIM);
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
                    
                    scannedFile.close();
                    file.close();
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