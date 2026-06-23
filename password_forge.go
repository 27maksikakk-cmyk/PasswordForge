// password_forge.go
package main

import (
	"flag"
	"fmt"
	"math"
	"math/rand"
	"os"
	"strings"
	"time"
)

// ANSI colors
const (
	reset  = "\033[0m"
	green  = "\033[92m"
	yellow = "\033[93m"
	red    = "\033[91m"
	blue   = "\033[94m"
)

func colorize(text, color string) string {
	return color + text + reset
}

// Встроенный словарь
var commonPasswords = map[string]bool{
	"password": true, "123456": true, "12345678": true, "1234": true,
	"qwerty": true, "12345": true, "dragon": true, "baseball": true,
	"football": true, "letmein": true, "monkey": true, "abc123": true,
	"mustang": true, "michael": true, "shadow": true, "master": true,
	"jennifer": true, "111111": true, "2000": true, "jordan": true,
	"superman": true, "harley": true, "1234567": true, "fuckyou": true,
	"hunter": true, "trustno1": true, "ranger": true, "buster": true,
	"thomas": true, "tigger": true, "robert": true, "soccer": true,
	"battle": true, "killer": true, "hockey": true, "george": true,
	"andrew": true, "charlie": true, "donald": true, "asshole": true,
	"qwertyuiop": true, "liverpool": true, "nicole": true, "daniel": true,
	"ginger": true, "whatever": true, "samantha": true, "jasmine": true,
	"passw0rd": true, "qazwsx": true,
}

func isCommon(pwd string) bool {
	return commonPasswords[strings.ToLower(pwd)]
}

func estimateEntropy(pwd string, charsetSize int) float64 {
	if charsetSize == 0 {
		unique := make(map[rune]bool)
		for _, c := range pwd {
			unique[c] = true
		}
		charsetSize = len(unique)
	}
	if charsetSize <= 1 {
		return 0
	}
	return float64(len(pwd)) * math.Log2(float64(charsetSize))
}

func formatTime(seconds float64) string {
	if math.IsInf(seconds, 1) || seconds > 1e30 {
		return "∞ (слишком долго)"
	}
	if seconds < 1 {
		return fmt.Sprintf("%.3f секунд", seconds)
	} else if seconds < 60 {
		return fmt.Sprintf("%.0f секунд", seconds)
	} else if seconds < 3600 {
		return fmt.Sprintf("%.0f минут", seconds/60)
	} else if seconds < 86400 {
		return fmt.Sprintf("%.0f часов", seconds/3600)
	} else if seconds < 31536000 {
		return fmt.Sprintf("%.0f дней", seconds/86400)
	} else {
		return fmt.Sprintf("%.0f лет", seconds/31536000)
	}
}

func generatePronounceable(length int, rng *rand.Rand) string {
	vowels := "aeiouy"
	consonants := "bcdfghjklmnpqrstvwxz"
	pwd := make([]byte, length)
	for i := 0; i < length; i++ {
		if i%2 == 0 {
			pwd[i] = consonants[rng.Intn(len(consonants))]
		} else {
			pwd[i] = vowels[rng.Intn(len(vowels))]
		}
	}
	if rng.Intn(2) == 0 && length > 0 {
		pwd[0] = byte(strings.ToUpper(string(pwd[0]))[0])
	}
	return string(pwd)
}

func generatePassword(length int, useUpper, useLower, useDigits, useSpecial bool,
	excludeAmbiguous, pronounceable bool, rng *rand.Rand) string {
	if pronounceable {
		return generatePronounceable(length, rng)
	}
	var chars []rune
	if useLower {
		chars = append(chars, []rune("abcdefghijklmnopqrstuvwxyz")...)
	}
	if useUpper {
		chars = append(chars, []rune("ABCDEFGHIJKLMNOPQRSTUVWXYZ")...)
	}
	if useDigits {
		chars = append(chars, []rune("0123456789")...)
	}
	if useSpecial {
		chars = append(chars, []rune("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~")...)
	}
	if len(chars) == 0 {
		panic("Ни один набор символов не выбран")
	}
	if excludeAmbiguous {
		ambiguous := map[rune]bool{'O': true, '0': true, 'o': true, '1': true, 'l': true, 'I': true}
		filtered := make([]rune, 0)
		for _, c := range chars {
			if !ambiguous[c] {
				filtered = append(filtered, c)
			}
		}
		chars = filtered
	}
	pwd := make([]rune, length)
	for i := 0; i < length; i++ {
		pwd[i] = chars[rng.Intn(len(chars))]
	}
	return string(pwd)
}

func main() {
	var length int
	var count int
	var uppercase bool
	var lowercase bool
	var digits bool
	var special bool
	var exclude bool
	var pronounce bool
	var minEntropy float64
	var quiet bool
	var output string
	var help bool

	flag.IntVar(&length, "l", 16, "Длина пароля")
	flag.IntVar(&count, "c", 1, "Количество паролей")
	flag.BoolVar(&uppercase, "u", true, "Включать заглавные")
	flag.BoolVar(&lowercase, "L", true, "Включать строчные")
	flag.BoolVar(&digits, "d", true, "Включать цифры")
	flag.BoolVar(&special, "s", true, "Включать спецсимволы")
	flag.BoolVar(&exclude, "e", false, "Исключать неоднозначные")
	flag.BoolVar(&pronounce, "p", false, "Произносимый режим")
	flag.Float64Var(&minEntropy, "m", 0, "Минимальная энтропия (бит)")
	flag.BoolVar(&quiet, "q", false, "Не выводить оценку")
	flag.StringVar(&output, "o", "", "Файл для сохранения")
	flag.BoolVar(&help, "h", false, "Справка")
	flag.Parse()

	if help {
		fmt.Println(`Usage: password_forge [options]
Options:
  -l <n>       Длина пароля (default 16)
  -c <n>       Количество паролей (default 1)
  -u           Включать заглавные (default on)
  -L           Включать строчные (default on)
  -d           Включать цифры (default on)
  -s           Включать спецсимволы (default on)
  -e           Исключать неоднозначные символы
  -p           Генерировать произносимые пароли
  -m <n>       Минимальная энтропия (бит)
  -q           Не выводить оценку сложности
  -o <file>    Сохранить в файл
  -h           Справка`)
		return
	}

	rng := rand.New(rand.NewSource(time.Now().UnixNano()))
	var passwords []string
	for i := 0; i < count; i++ {
		var pwd string
		attempts := 0
		for {
			pwd = generatePassword(length, uppercase, lowercase, digits, special,
				exclude, pronounce, rng)
			attempts++
			if attempts > 1000 {
				panic("Не удалось сгенерировать пароль с нужной энтропией")
			}
			if estimateEntropy(pwd, 0) >= minEntropy {
				break
			}
		}
		passwords = append(passwords, pwd)
	}

	var outputText string
	for _, pwd := range passwords {
		entropy := estimateEntropy(pwd, 0)
		common := isCommon(pwd)
		crackSec := math.Pow(2, entropy) / 1e9
		if entropy == 0 {
			crackSec = math.Inf(1)
		}
		crackStr := formatTime(crackSec)
		var strength string
		if entropy < 30 {
			strength = colorize("Слабый", red)
		} else if entropy < 50 {
			strength = colorize("Средний", yellow)
		} else {
			strength = colorize("Сильный", green)
		}

		outputText += fmt.Sprintf("Пароль: %s\n", pwd)
		if !quiet {
			outputText += fmt.Sprintf("  Энтропия: %.0f бит, Стойкость: %s", entropy, strength)
			outputText += fmt.Sprintf(", Время взлома: %s", crackStr)
			if common {
				outputText += colorize(" (Словарный!)", red)
			}
			outputText += "\n\n"
		} else {
			outputText += "\n"
		}
	}

	if output != "" {
		err := os.WriteFile(output, []byte(outputText), 0644)
		if err != nil {
			fmt.Println(colorize("Ошибка записи файла: "+err.Error(), red))
			os.Exit(1)
		}
		fmt.Println(colorize("Результат сохранён в "+output, green))
	} else {
		fmt.Print(outputText)
	}
}
