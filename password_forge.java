// password_forge.java
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;

public class password_forge {
    // ANSI colors
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[92m";
    private static final String YELLOW = "\u001B[93m";
    private static final String RED = "\u001B[91m";
    private static final String BLUE = "\u001B[94m";

    private static String colorize(String text, String color) {
        return color + text + RESET;
    }

    // Встроенный словарь
    private static final Set<String> COMMON_PASSWORDS = new HashSet<>(Arrays.asList(
        "password", "123456", "12345678", "1234", "qwerty", "12345", "dragon",
        "baseball", "football", "letmein", "monkey", "abc123", "mustang", "michael",
        "shadow", "master", "jennifer", "111111", "2000", "jordan", "superman",
        "harley", "1234567", "fuckyou", "hunter", "trustno1", "ranger", "buster",
        "thomas", "tigger", "robert", "soccer", "battle", "killer", "hockey", "george",
        "andrew", "charlie", "donald", "asshole", "qwertyuiop", "liverpool", "nicole",
        "daniel", "ginger", "whatever", "samantha", "jasmine", "passw0rd", "qazwsx"
    ));

    private static boolean isCommon(String pwd) {
        return COMMON_PASSWORDS.contains(pwd.toLowerCase());
    }

    private static double estimateEntropy(String pwd, int charsetSize) {
        if (charsetSize == 0) {
            Set<Character> unique = new HashSet<>();
            for (char c : pwd.toCharArray()) unique.add(c);
            charsetSize = unique.size();
        }
        if (charsetSize <= 1) return 0;
        return pwd.length() * (Math.log(charsetSize) / Math.log(2));
    }

    private static String formatTime(double seconds) {
        if (Double.isInfinite(seconds)) return "∞ (слишком долго)";
        if (seconds < 1) return String.format("%.3f секунд", seconds);
        if (seconds < 60) return String.format("%.0f секунд", seconds);
        if (seconds < 3600) return String.format("%.0f минут", seconds/60);
        if (seconds < 86400) return String.format("%.0f часов", seconds/3600);
        if (seconds < 31536000) return String.format("%.0f дней", seconds/86400);
        return String.format("%.0f лет", seconds/31536000);
    }

    private static String generatePronounceable(int length, SecureRandom rng) {
        final String vowels = "aeiouy";
        final String consonants = "bcdfghjklmnpqrstvwxz";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            if (i % 2 == 0)
                sb.append(consonants.charAt(rng.nextInt(consonants.length())));
            else
                sb.append(vowels.charAt(rng.nextInt(vowels.length())));
        }
        if (rng.nextBoolean() && length > 0)
            sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        return sb.toString();
    }

    private static String generatePassword(int length, boolean useUpper, boolean useLower,
                                           boolean useDigits, boolean useSpecial,
                                           boolean excludeAmbiguous, boolean pronounceable,
                                           SecureRandom rng) {
        if (pronounceable) return generatePronounceable(length, rng);

        List<Character> chars = new ArrayList<>();
        if (useLower) for (char c : "abcdefghijklmnopqrstuvwxyz".toCharArray()) chars.add(c);
        if (useUpper) for (char c : "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()) chars.add(c);
        if (useDigits) for (char c : "0123456789".toCharArray()) chars.add(c);
        if (useSpecial) for (char c : "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toCharArray()) chars.add(c);
        if (chars.isEmpty()) throw new IllegalArgumentException("Ни один набор символов не выбран");

        if (excludeAmbiguous) {
            Set<Character> ambiguous = new HashSet<>(Arrays.asList('O','0','o','1','l','I'));
            chars.removeIf(ambiguous::contains);
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(chars.get(rng.nextInt(chars.size())));
        return sb.toString();
    }

    private static void printHelp() {
        System.out.println("Usage: java password_forge [options]");
        System.out.println("Options:");
        System.out.println("  -l, --length <n>       Длина пароля (default 16)");
        System.out.println("  -c, --count <n>        Количество паролей (default 1)");
        System.out.println("  -u, --uppercase        Включать заглавные (default on)");
        System.out.println("  -L, --lowercase        Включать строчные (default on)");
        System.out.println("  -d, --digits           Включать цифры (default on)");
        System.out.println("  -s, --special          Включать спецсимволы (default on)");
        System.out.println("  -e, --exclude          Исключать неоднозначные символы");
        System.out.println("  -p, --pronounce        Генерировать произносимые пароли");
        System.out.println("  -m, --min-entropy <n>  Минимальная энтропия (бит)");
        System.out.println("  -q, --quiet            Не выводить оценку сложности");
        System.out.println("  -o, --output <file>    Сохранить в файл");
        System.out.println("  -h, --help             Справка");
    }

    public static void main(String[] args) {
        int length = 16, count = 1;
        boolean useUpper = true, useLower = true, useDigits = true, useSpecial = true;
        boolean excludeAmbiguous = false, pronounceable = false, quiet = false;
        double minEntropy = 0;
        String outputFile = null;
        boolean help = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-l":
                case "--length": length = Integer.parseInt(args[++i]); break;
                case "-c":
                case "--count": count = Integer.parseInt(args[++i]); break;
                case "-u":
                case "--uppercase": useUpper = true; break;
                case "-L":
                case "--lowercase": useLower = true; break;
                case "-d":
                case "--digits": useDigits = true; break;
                case "-s":
                case "--special": useSpecial = true; break;
                case "-e":
                case "--exclude": excludeAmbiguous = true; break;
                case "-p":
                case "--pronounce": pronounceable = true; break;
                case "-m":
                case "--min-entropy": minEntropy = Double.parseDouble(args[++i]); break;
                case "-q":
                case "--quiet": quiet = true; break;
                case "-o":
                case "--output": outputFile = args[++i]; break;
                case "-h":
                case "--help": help = true; break;
                default:
                    System.err.println(colorize("Неизвестная опция: " + arg, RED));
                    System.exit(1);
            }
        }

        if (help) { printHelp(); return; }

        SecureRandom rng = new SecureRandom();
        List<String> passwords = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String pwd;
            int attempts = 0;
            do {
                pwd = generatePassword(length, useUpper, useLower, useDigits, useSpecial,
                                       excludeAmbiguous, pronounceable, rng);
                attempts++;
                if (attempts > 1000) throw new RuntimeException("Не удалось сгенерировать пароль с нужной энтропией");
            } while (estimateEntropy(pwd, 0) < minEntropy);
            passwords.add(pwd);
        }

        StringBuilder output = new StringBuilder();
        for (String pwd : passwords) {
            double entropy = estimateEntropy(pwd, 0);
            boolean common = isCommon(pwd);
            double crackSec = entropy == 0 ? Double.POSITIVE_INFINITY : Math.pow(2, entropy) / 1e9;
            String crackStr = formatTime(crackSec);
            String strength;
            if (entropy < 30) strength = colorize("Слабый", RED);
            else if (entropy < 50) strength = colorize("Средний", YELLOW);
            else strength = colorize("Сильный", GREEN);

            output.append("Пароль: ").append(pwd).append("\n");
            if (!quiet) {
                output.append(String.format("  Энтропия: %.0f бит, Стойкость: %s", entropy, strength));
                output.append(", Время взлома: ").append(crackStr);
                if (common) output.append(colorize(" (Словарный!)", RED));
                output.append("\n\n");
            } else {
                output.append("\n");
            }
        }

        if (outputFile != null && !outputFile.isEmpty()) {
            try {
                Files.write(Paths.get(outputFile), output.toString().getBytes(StandardCharsets.UTF_8));
                System.out.println(colorize("Результат сохранён в " + outputFile, GREEN));
            } catch (IOException e) {
                System.err.println(colorize("Ошибка записи файла: " + e.getMessage(), RED));
                System.exit(1);
            }
        } else {
            System.out.print(output.toString());
        }
    }
}
