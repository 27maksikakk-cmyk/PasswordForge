// password_forge.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;

class PasswordForge
{
    // ANSI colors
    static string Colorize(string text, string color)
    {
        string col = color switch
        {
            "green" => "\x1b[92m",
            "yellow" => "\x1b[93m",
            "red" => "\x1b[91m",
            "blue" => "\x1b[94m",
            _ => "\x1b[0m"
        };
        return col + text + "\x1b[0m";
    }

    // Встроенный словарь
    static readonly HashSet<string> CommonPasswords = new HashSet<string>
    {
        "password", "123456", "12345678", "1234", "qwerty", "12345", "dragon",
        "baseball", "football", "letmein", "monkey", "abc123", "mustang", "michael",
        "shadow", "master", "jennifer", "111111", "2000", "jordan", "superman",
        "harley", "1234567", "fuckyou", "hunter", "trustno1", "ranger", "buster",
        "thomas", "tigger", "robert", "soccer", "battle", "killer", "hockey", "george",
        "andrew", "charlie", "donald", "asshole", "qwertyuiop", "liverpool", "nicole",
        "daniel", "ginger", "whatever", "samantha", "jasmine", "passw0rd", "qazwsx"
    };

    static bool IsCommon(string pwd) => CommonPasswords.Contains(pwd.ToLower());

    static double EstimateEntropy(string pwd, int charsetSize = 0)
    {
        if (charsetSize == 0)
        {
            var unique = new HashSet<char>(pwd);
            charsetSize = unique.Count;
        }
        if (charsetSize <= 1) return 0;
        return pwd.Length * Math.Log2(charsetSize);
    }

    static string FormatTime(double seconds)
    {
        if (double.IsInfinity(seconds)) return "∞ (слишком долго)";
        if (seconds < 1) return $"{seconds:F3} секунд";
        if (seconds < 60) return $"{seconds:F0} секунд";
        if (seconds < 3600) return $"{(seconds/60):F0} минут";
        if (seconds < 86400) return $"{(seconds/3600):F0} часов";
        if (seconds < 31536000) return $"{(seconds/86400):F0} дней";
        return $"{(seconds/31536000):F0} лет";
    }

    static string GeneratePronounceable(int length, Random rng)
    {
        const string vowels = "aeiouy";
        const string consonants = "bcdfghjklmnpqrstvwxz";
        var sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
        {
            if (i % 2 == 0)
                sb.Append(consonants[rng.Next(consonants.Length)]);
            else
                sb.Append(vowels[rng.Next(vowels.Length)]);
        }
        if (rng.Next(2) == 0 && length > 0)
            sb[0] = char.ToUpper(sb[0]);
        return sb.ToString();
    }

    static string GeneratePassword(int length, bool useUpper, bool useLower, bool useDigits, bool useSpecial,
                                   bool excludeAmbiguous, bool pronounceable, Random rng)
    {
        if (pronounceable) return GeneratePronounceable(length, rng);

        var chars = new List<char>();
        if (useLower) chars.AddRange("abcdefghijklmnopqrstuvwxyz");
        if (useUpper) chars.AddRange("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        if (useDigits) chars.AddRange("0123456789");
        if (useSpecial) chars.AddRange("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~");
        if (chars.Count == 0) throw new Exception("Ни один набор символов не выбран");

        if (excludeAmbiguous)
        {
            var ambiguous = new HashSet<char>("O0o1lI");
            chars.RemoveAll(c => ambiguous.Contains(c));
        }

        var sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.Append(chars[rng.Next(chars.Count)]);
        return sb.ToString();
    }

    static void Main(string[] args)
    {
        int length = 16, count = 1;
        bool useUpper = true, useLower = true, useDigits = true, useSpecial = true;
        bool excludeAmbiguous = false, pronounceable = false, quiet = false;
        double minEntropy = 0;
        string outputFile = null;
        bool help = false;

        for (int i = 0; i < args.Length; i++)
        {
            string arg = args[i];
            switch (arg)
            {
                case "-l":
                case "--length": length = int.Parse(args[++i]); break;
                case "-c":
                case "--count": count = int.Parse(args[++i]); break;
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
                case "--min-entropy": minEntropy = double.Parse(args[++i]); break;
                case "-q":
                case "--quiet": quiet = true; break;
                case "-o":
                case "--output": outputFile = args[++i]; break;
                case "-h":
                case "--help": help = true; break;
                default:
                    Console.WriteLine(Colorize($"Неизвестная опция: {arg}", "red"));
                    Environment.Exit(1);
                    break;
            }
        }

        if (help)
        {
            Console.WriteLine(@"Usage: password_forge [options]
Options:
  -l, --length <n>       Длина пароля (default 16)
  -c, --count <n>        Количество паролей (default 1)
  -u, --uppercase        Включать заглавные (default on)
  -L, --lowercase        Включать строчные (default on)
  -d, --digits           Включать цифры (default on)
  -s, --special          Включать спецсимволы (default on)
  -e, --exclude          Исключать неоднозначные символы
  -p, --pronounce        Генерировать произносимые пароли
  -m, --min-entropy <n>  Минимальная энтропия (бит)
  -q, --quiet            Не выводить оценку сложности
  -o, --output <file>    Сохранить в файл
  -h, --help             Справка");
            return;
        }

        var rng = new Random();
        var passwords = new List<string>();
        for (int i = 0; i < count; i++)
        {
            string pwd;
            int attempts = 0;
            do
            {
                pwd = GeneratePassword(length, useUpper, useLower, useDigits, useSpecial,
                                       excludeAmbiguous, pronounceable, rng);
                attempts++;
                if (attempts > 1000) throw new Exception("Не удалось сгенерировать пароль с нужной энтропией");
            } while (EstimateEntropy(pwd) < minEntropy);
            passwords.Add(pwd);
        }

        var output = new StringBuilder();
        foreach (var pwd in passwords)
        {
            double entropy = EstimateEntropy(pwd);
            bool common = IsCommon(pwd);
            double crackSec = entropy == 0 ? double.PositiveInfinity : Math.Pow(2, entropy) / 1e9;
            string crackStr = FormatTime(crackSec);
            string strength;
            if (entropy < 30) strength = Colorize("Слабый", "red");
            else if (entropy < 50) strength = Colorize("Средний", "yellow");
            else strength = Colorize("Сильный", "green");

            output.AppendLine($"Пароль: {pwd}");
            if (!quiet)
            {
                output.Append($"  Энтропия: {entropy:F0} бит, Стойкость: {strength}");
                output.Append($", Время взлома: {crackStr}");
                if (common) output.Append(Colorize(" (Словарный!)", "red"));
                output.AppendLine("\n");
            }
            else
            {
                output.AppendLine();
            }
        }

        if (!string.IsNullOrEmpty(outputFile))
        {
            try
            {
                File.WriteAllText(outputFile, output.ToString(), Encoding.UTF8);
                Console.WriteLine(Colorize($"Результат сохранён в {outputFile}", "green"));
            }
            catch (Exception e)
            {
                Console.WriteLine(Colorize($"Ошибка записи файла: {e.Message}", "red"));
                Environment.Exit(1);
            }
        }
        else
        {
            Console.Write(output.ToString());
        }
    }
}
