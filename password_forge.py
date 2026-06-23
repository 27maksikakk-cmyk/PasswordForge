# password_forge.py
#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import random
import string
import sys
import math
import os
from collections import Counter

# ANSI colors
COLORS = {
    'green': '\033[92m',
    'yellow': '\033[93m',
    'red': '\033[91m',
    'blue': '\033[94m',
    'reset': '\033[0m'
}

def colorize(text, color):
    return f"{COLORS.get(color, '')}{text}{COLORS['reset']}"

# Встроенный словарь топ-1000 (для демонстрации, сокращённый)
COMMON_PASSWORDS = {
    'password', '123456', '12345678', '1234', 'qwerty', '12345', 'dragon',
    'baseball', 'football', 'letmein', 'monkey', 'abc123', 'mustang', 'michael',
    'shadow', 'master', 'jennifer', '111111', '2000', 'jordan', 'superman',
    'harley', '1234567', 'fuckyou', 'hunter', 'trustno1', 'ranger', 'buster',
    'thomas', 'tigger', 'robert', 'soccer', 'battle', 'killer', 'hockey', 'george',
    'andrew', 'charlie', 'donald', 'asshole', 'qwertyuiop', 'liverpool', 'nicole',
    'daniel', 'ginger', 'whatever', 'samantha', 'jasmine', 'passw0rd', 'qazwsx'
}

def load_common_passwords():
    return COMMON_PASSWORDS

def check_dictionary(password):
    return password.lower() in load_common_passwords()

def estimate_entropy(password, charset_size=None):
    """Вычисляет энтропию пароля (бит). Если charset_size не указан, вычисляется по символам."""
    if charset_size is None:
        unique_chars = set(password)
        charset_size = len(unique_chars)
    if charset_size <= 1:
        return 0
    return len(password) * math.log2(charset_size)

def estimate_crack_time(entropy, attempts_per_second=1e9):
    """Приблизительное время взлома в секундах."""
    if entropy == 0:
        return float('inf')
    return (2 ** entropy) / attempts_per_second

def format_time(seconds):
    if seconds == float('inf'):
        return "∞ (слишком долго)"
    if seconds < 1:
        return f"{seconds:.3f} секунд"
    elif seconds < 60:
        return f"{seconds:.1f} секунд"
    elif seconds < 3600:
        return f"{seconds/60:.1f} минут"
    elif seconds < 86400:
        return f"{seconds/3600:.1f} часов"
    elif seconds < 31536000:
        return f"{seconds/86400:.1f} дней"
    else:
        return f"{seconds/31536000:.1f} лет"

def generate_pronounceable(length):
    """Генерирует произносимый пароль (согласная-гласная)."""
    vowels = 'aeiouy'
    consonants = 'bcdfghjklmnpqrstvwxz'
    password = []
    for i in range(length):
        if i % 2 == 0:
            password.append(random.choice(consonants))
        else:
            password.append(random.choice(vowels))
    # Иногда добавляем заглавную в начало
    if random.random() < 0.5:
        password[0] = password[0].upper()
    return ''.join(password)

def generate_password(length, use_upper=True, use_lower=True, use_digits=True, use_special=True,
                      exclude_ambiguous=False, pronounceable=False):
    if pronounceable:
        return generate_pronounceable(length)

    chars = ''
    if use_lower:
        chars += string.ascii_lowercase
    if use_upper:
        chars += string.ascii_uppercase
    if use_digits:
        chars += string.digits
    if use_special:
        chars += string.punctuation

    if not chars:
        raise ValueError("Хотя бы один набор символов должен быть включён")

    if exclude_ambiguous:
        ambiguous = 'O0o1lI'
        chars = ''.join(c for c in chars if c not in ambiguous)

    # Если длина больше доступных символов, повторяем
    if length > len(chars):
        # Для простоты генерируем с повторениями
        return ''.join(random.choice(chars) for _ in range(length))
    else:
        return ''.join(random.sample(chars, length))

def main():
    parser = argparse.ArgumentParser(description="PasswordForge – генератор паролей с оценкой сложности")
    parser.add_argument('-l', '--length', type=int, default=16, help='Длина пароля (по умолчанию 16)')
    parser.add_argument('-c', '--count', type=int, default=1, help='Количество паролей (по умолчанию 1)')
    parser.add_argument('-u', '--uppercase', action='store_true', default=True, help='Включать заглавные (по умолчанию да)')
    parser.add_argument('-L', '--lowercase', action='store_true', default=True, help='Включать строчные (по умолчанию да)')
    parser.add_argument('-d', '--digits', action='store_true', default=True, help='Включать цифры (по умолчанию да)')
    parser.add_argument('-s', '--special', action='store_true', default=True, help='Включать спецсимволы (по умолчанию да)')
    parser.add_argument('-e', '--exclude', action='store_true', help='Исключать неоднозначные символы')
    parser.add_argument('-p', '--pronounce', action='store_true', help='Генерировать произносимые пароли')
    parser.add_argument('-m', '--min-entropy', type=float, default=0,
                        help='Минимальная энтропия в битах (повторять генерацию до достижения)')
    parser.add_argument('-q', '--quiet', action='store_true', help='Не выводить оценку сложности')
    parser.add_argument('-o', '--output', help='Сохранить результат в файл')
    args = parser.parse_args()

    # Если включен произносимый режим, игнорируем другие наборы
    if args.pronounce:
        args.uppercase = True
        args.lowercase = True
        args.digits = False
        args.special = False

    # Генерация паролей
    passwords = []
    for _ in range(args.count):
        password = ''
        attempts = 0
        while True:
            password = generate_password(
                args.length,
                args.uppercase, args.lowercase, args.digits, args.special,
                args.exclude, args.pronounce
            )
            entropy = estimate_entropy(password)
            if entropy >= args.min_entropy:
                break
            attempts += 1
            if attempts > 1000:
                raise RuntimeError("Не удалось сгенерировать пароль с достаточной энтропией")
        passwords.append(password)

    # Формирование вывода
    output_lines = []
    for pwd in passwords:
        entropy = estimate_entropy(pwd)
        is_common = check_dictionary(pwd)
        crack_seconds = estimate_crack_time(entropy)
        crack_str = format_time(crack_seconds)

        # Оценка стойкости
        if entropy < 30:
            strength = colorize("Слабый", 'red')
        elif entropy < 50:
            strength = colorize("Средний", 'yellow')
        else:
            strength = colorize("Сильный", 'green')

        line = f"Пароль: {pwd}"
        if not args.quiet:
            line += f"\n  Энтропия: {entropy:.1f} бит, Стойкость: {strength}"
            line += f", Время взлома: {crack_str}"
            if is_common:
                line += colorize(" (Словарный!)", 'red')
        output_lines.append(line)

    output_text = "\n\n".join(output_lines)

    # Вывод
    if args.output:
        try:
            with open(args.output, 'w', encoding='utf-8') as f:
                f.write(output_text)
            print(colorize(f"Результат сохранён в {args.output}", 'green'))
        except Exception as e:
            print(colorize(f"Ошибка записи файла: {e}", 'red'), file=sys.stderr)
            sys.exit(1)
    else:
        print(output_text)

if __name__ == '__main__':
    main()
