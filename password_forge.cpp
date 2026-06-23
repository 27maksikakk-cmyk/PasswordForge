// password_forge.cpp
#include <iostream>
#include <string>
#include <vector>
#include <random>
#include <algorithm>
#include <cmath>
#include <fstream>
#include <cctype>
#include <unordered_set>
#include <getopt.h>

using namespace std;

// ANSI colors
const string RESET = "\033[0m";
const string GREEN = "\033[92m";
const string YELLOW = "\033[93m";
const string RED = "\033[91m";
const string BLUE = "\033[94m";

string colorize(const string& text, const string& color) {
    return color + text + RESET;
}

// Встроенный словарь (топ-1000 урезанный)
unordered_set<string> common_passwords = {
    "password", "123456", "12345678", "1234", "qwerty", "12345", "dragon",
    "baseball", "football", "letmein", "monkey", "abc123", "mustang", "michael",
    "shadow", "master", "jennifer", "111111", "2000", "jordan", "superman",
    "harley", "1234567", "fuckyou", "hunter", "trustno1", "ranger", "buster",
    "thomas", "tigger", "robert", "soccer", "battle", "killer", "hockey", "george",
    "andrew", "charlie", "donald", "asshole", "qwertyuiop", "liverpool", "nicole",
    "daniel", "ginger", "whatever", "samantha", "jasmine", "passw0rd", "qazwsx"
};

bool is_common_password(const string& pwd) {
    string low = pwd;
    transform(low.begin(), low.end(), low.begin(), ::tolower);
    return common_passwords.find(low) != common_passwords.end();
}

double estimate_entropy(const string& pwd, int charset_size = 0) {
    if (charset_size == 0) {
        unordered_set<char> unique(pwd.begin(), pwd.end());
        charset_size = unique.size();
    }
    if (charset_size <= 1) return 0;
    return pwd.length() * log2(charset_size);
}

string format_time(double seconds) {
    if (seconds == INFINITY) return "∞ (слишком долго)";
    if (seconds < 1) return to_string(seconds) + " секунд";
    else if (seconds < 60) return to_string((int)seconds) + " секунд";
    else if (seconds < 3600) return to_string((int)(seconds/60)) + " минут";
    else if (seconds < 86400) return to_string((int)(seconds/3600)) + " часов";
    else if (seconds < 31536000) return to_string((int)(seconds/86400)) + " дней";
    else return to_string((int)(seconds/31536000)) + " лет";
}

string generate_pronounceable(int length, mt19937& rng) {
    const string vowels = "aeiouy";
    const string consonants = "bcdfghjklmnpqrstvwxz";
    uniform_int_distribution<> vowel_dist(0, vowels.size()-1);
    uniform_int_distribution<> cons_dist(0, consonants.size()-1);
    string pwd;
    for (int i = 0; i < length; ++i) {
        if (i % 2 == 0)
            pwd += consonants[cons_dist(rng)];
        else
            pwd += vowels[vowel_dist(rng)];
    }
    if (length > 0 && (rng() % 2 == 0))
        pwd[0] = toupper(pwd[0]);
    return pwd;
}

string generate_password(int length, bool use_upper, bool use_lower, bool use_digits, bool use_special,
                          bool exclude_ambiguous, bool pronounceable, mt19937& rng) {
    if (pronounceable) return generate_pronounceable(length, rng);
    string chars;
    if (use_lower) chars += "abcdefghijklmnopqrstuvwxyz";
    if (use_upper) chars += "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    if (use_digits) chars += "0123456789";
    if (use_special) chars += "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
    if (chars.empty()) throw runtime_error("Ни один набор символов не выбран");
    if (exclude_ambiguous) {
        string ambiguous = "O0o1lI";
        for (char c : ambiguous)
            chars.erase(remove(chars.begin(), chars.end(), c), chars.end());
    }
    uniform_int_distribution<> dist(0, chars.size()-1);
    string pwd;
    for (int i = 0; i < length; ++i)
        pwd += chars[dist(rng)];
    return pwd;
}

int main(int argc, char* argv[]) {
    int length = 16, count = 1;
    bool use_upper = true, use_lower = true, use_digits = true, use_special = true;
    bool exclude_ambiguous = false, pronounceable = false, quiet = false;
    double min_entropy = 0;
    string output_file;

    static struct option long_options[] = {
        {"length", required_argument, 0, 'l'},
        {"count", required_argument, 0, 'c'},
        {"uppercase", no_argument, 0, 'u'},
        {"lowercase", no_argument, 0, 'L'},
        {"digits", no_argument, 0, 'd'},
        {"special", no_argument, 0, 's'},
        {"exclude", no_argument, 0, 'e'},
        {"pronounce", no_argument, 0, 'p'},
        {"min-entropy", required_argument, 0, 'm'},
        {"quiet", no_argument, 0, 'q'},
        {"output", required_argument, 0, 'o'},
        {"help", no_argument, 0, 'h'},
        {0,0,0,0}
    };

    int opt;
    while ((opt = getopt_long(argc, argv, "l:c:uLdsep:m:qo:h", long_options, nullptr)) != -1) {
        switch (opt) {
            case 'l': length = stoi(optarg); break;
            case 'c': count = stoi(optarg); break;
            case 'u': use_upper = true; break;
            case 'L': use_lower = true; break;
            case 'd': use_digits = true; break;
            case 's': use_special = true; break;
            case 'e': exclude_ambiguous = true; break;
            case 'p': pronounceable = true; break;
            case 'm': min_entropy = stod(optarg); break;
            case 'q': quiet = true; break;
            case 'o': output_file = optarg; break;
            case 'h':
                cout << "Usage: password_forge [options]\n"
                     << "Options:\n"
                     << "  -l, --length <n>       Длина пароля (default 16)\n"
                     << "  -c, --count <n>        Количество паролей (default 1)\n"
                     << "  -u, --uppercase        Включать заглавные (default on)\n"
                     << "  -L, --lowercase        Включать строчные (default on)\n"
                     << "  -d, --digits           Включать цифры (default on)\n"
                     << "  -s, --special          Включать спецсимволы (default on)\n"
                     << "  -e, --exclude          Исключать неоднозначные символы\n"
                     << "  -p, --pronounce        Генерировать произносимые пароли\n"
                     << "  -m, --min-entropy <n>  Минимальная энтропия (бит)\n"
                     << "  -q, --quiet            Не выводить оценку сложности\n"
                     << "  -o, --output <file>    Сохранить в файл\n"
                     << "  -h, --help             Справка\n";
                return 0;
            default: return 1;
        }
    }

    random_device rd;
    mt19937 rng(rd());

    vector<string> passwords;
    for (int i = 0; i < count; ++i) {
        string pwd;
        int attempts = 0;
        do {
            pwd = generate_password(length, use_upper, use_lower, use_digits, use_special,
                                    exclude_ambiguous, pronounceable, rng);
            attempts++;
            if (attempts > 1000) throw runtime_error("Не удалось сгенерировать пароль с нужной энтропией");
        } while (estimate_entropy(pwd) < min_entropy);
        passwords.push_back(pwd);
    }

    string output;
    for (const auto& pwd : passwords) {
        double entropy = estimate_entropy(pwd);
        bool common = is_common_password(pwd);
        double crack_sec = (entropy == 0) ? INFINITY : pow(2, entropy) / 1e9;
        string crack_str = format_time(crack_sec);
        string strength;
        if (entropy < 30) strength = colorize("Слабый", RED);
        else if (entropy < 50) strength = colorize("Средний", YELLOW);
        else strength = colorize("Сильный", GREEN);

        output += "Пароль: " + pwd + "\n";
        if (!quiet) {
            output += "  Энтропия: " + to_string((int)entropy) + " бит, Стойкость: " + strength;
            output += ", Время взлома: " + crack_str;
            if (common) output += colorize(" (Словарный!)", RED);
            output += "\n\n";
        } else {
            output += "\n";
        }
    }

    if (!output_file.empty()) {
        ofstream f(output_file);
        if (!f) { cerr << colorize("Ошибка записи файла", RED) << endl; return 1; }
        f << output;
        cout << colorize("Результат сохранён в " + output_file, GREEN) << endl;
    } else {
        cout << output;
    }
    return 0;
}
