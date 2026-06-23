// password_forge.js
#!/usr/bin/env node
'use strict';

const crypto = require('crypto');
const fs = require('fs');

// ANSI colors
const COLORS = {
    green: '\x1b[92m',
    yellow: '\x1b[93m',
    red: '\x1b[91m',
    blue: '\x1b[94m',
    reset: '\x1b[0m'
};

function colorize(text, color) {
    return COLORS[color] + text + COLORS.reset;
}

// Встроенный словарь (топ-1000 урезанный)
const commonPasswords = new Set([
    'password', '123456', '12345678', '1234', 'qwerty', '12345', 'dragon',
    'baseball', 'football', 'letmein', 'monkey', 'abc123', 'mustang', 'michael',
    'shadow', 'master', 'jennifer', '111111', '2000', 'jordan', 'superman',
    'harley', '1234567', 'fuckyou', 'hunter', 'trustno1', 'ranger', 'buster',
    'thomas', 'tigger', 'robert', 'soccer', 'battle', 'killer', 'hockey', 'george',
    'andrew', 'charlie', 'donald', 'asshole', 'qwertyuiop', 'liverpool', 'nicole',
    'daniel', 'ginger', 'whatever', 'samantha', 'jasmine', 'passw0rd', 'qazwsx'
]);

function isCommon(pwd) {
    return commonPasswords.has(pwd.toLowerCase());
}

function estimateEntropy(pwd, charsetSize) {
    if (!charsetSize) {
        const unique = new Set(pwd);
        charsetSize = unique.size;
    }
    if (charsetSize <= 1) return 0;
    return pwd.length * Math.log2(charsetSize);
}

function formatTime(seconds) {
    if (!isFinite(seconds)) return '∞ (слишком долго)';
    if (seconds < 1) return seconds.toFixed(3) + ' секунд';
    if (seconds < 60) return Math.round(seconds) + ' секунд';
    if (seconds < 3600) return Math.round(seconds/60) + ' минут';
    if (seconds < 86400) return Math.round(seconds/3600) + ' часов';
    if (seconds < 31536000) return Math.round(seconds/86400) + ' дней';
    return Math.round(seconds/31536000) + ' лет';
}

function generatePronounceable(length) {
    const vowels = 'aeiouy';
    const consonants = 'bcdfghjklmnpqrstvwxz';
    let pwd = '';
    for (let i = 0; i < length; i++) {
        if (i % 2 === 0) pwd += consonants[Math.floor(Math.random() * consonants.length)];
        else pwd += vowels[Math.floor(Math.random() * vowels.length)];
    }
    if (Math.random() < 0.5) pwd = pwd[0].toUpperCase() + pwd.slice(1);
    return pwd;
}

function generatePassword(length, useUpper, useLower, useDigits, useSpecial,
                          excludeAmbiguous, pronounceable) {
    if (pronounceable) return generatePronounceable(length);

    let chars = '';
    if (useLower) chars += 'abcdefghijklmnopqrstuvwxyz';
    if (useUpper) chars += 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    if (useDigits) chars += '0123456789';
    if (useSpecial) chars += '!\"#$%&\'()*+,-./:;<=>?@[\\]^_`{|}~';
    if (!chars) throw new Error('Ни один набор символов не выбран');

    if (excludeAmbiguous) {
        const ambiguous = 'O0o1lI';
        chars = chars.split('').filter(c => !ambiguous.includes(c)).join('');
    }

    let pwd = '';
    for (let i = 0; i < length; i++) {
        pwd += chars[Math.floor(Math.random() * chars.length)];
    }
    return pwd;
}

function parseArgs() {
    const args = process.argv.slice(2);
    const opts = {
        length: 16,
        count: 1,
        uppercase: true,
        lowercase: true,
        digits: true,
        special: true,
        exclude: false,
        pronounce: false,
        minEntropy: 0,
        quiet: false,
        output: '',
        help: false
    };
    for (let i = 0; i < args.length; i++) {
        const arg = args[i];
        switch (arg) {
            case '-l':
            case '--length': opts.length = parseInt(args[++i]); break;
            case '-c':
            case '--count': opts.count = parseInt(args[++i]); break;
            case '-u':
            case '--uppercase': opts.uppercase = true; break;
            case '-L':
            case '--lowercase': opts.lowercase = true; break;
            case '-d':
            case '--digits': opts.digits = true; break;
            case '-s':
            case '--special': opts.special = true; break;
            case '-e':
            case '--exclude': opts.exclude = true; break;
            case '-p':
            case '--pronounce': opts.pronounce = true; break;
            case '-m':
            case '--min-entropy': opts.minEntropy = parseFloat(args[++i]); break;
            case '-q':
            case '--quiet': opts.quiet = true; break;
            case '-o':
            case '--output': opts.output = args[++i]; break;
            case '-h':
            case '--help': opts.help = true; break;
            default: console.error(colorize(`Неизвестная опция: ${arg}`, 'red')); process.exit(1);
        }
    }
    return opts;
}

function main() {
    const opts = parseArgs();
    if (opts.help) {
        console.log(`Usage: node password_forge.js [options]
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
  -h, --help             Справка`);
        return;
    }

    const passwords = [];
    for (let i = 0; i < opts.count; i++) {
        let pwd, attempts = 0;
        do {
            pwd = generatePassword(
                opts.length, opts.uppercase, opts.lowercase, opts.digits, opts.special,
                opts.exclude, opts.pronounce
            );
            attempts++;
            if (attempts > 1000) throw new Error('Не удалось сгенерировать пароль с нужной энтропией');
        } while (estimateEntropy(pwd) < opts.minEntropy);
        passwords.push(pwd);
    }

    let output = '';
    for (const pwd of passwords) {
        const entropy = estimateEntropy(pwd);
        const common = isCommon(pwd);
        const crackSec = entropy === 0 ? Infinity : Math.pow(2, entropy) / 1e9;
        const crackStr = formatTime(crackSec);
        let strength;
        if (entropy < 30) strength = colorize('Слабый', 'red');
        else if (entropy < 50) strength = colorize('Средний', 'yellow');
        else strength = colorize('Сильный', 'green');

        output += `Пароль: ${pwd}\n`;
        if (!opts.quiet) {
            output += `  Энтропия: ${Math.round(entropy)} бит, Стойкость: ${strength}`;
            output += `, Время взлома: ${crackStr}`;
            if (common) output += colorize(' (Словарный!)', 'red');
            output += '\n\n';
        } else {
            output += '\n';
        }
    }

    if (opts.output) {
        try {
            fs.writeFileSync(opts.output, output, 'utf8');
            console.log(colorize(`Результат сохранён в ${opts.output}`, 'green'));
        } catch (err) {
            console.error(colorize(`Ошибка записи файла: ${err.message}`, 'red'));
            process.exit(1);
        }
    } else {
        console.log(output);
    }
}

main().catch(err => {
    console.error(colorize(`Ошибка: ${err.message}`, 'red'));
    process.exit(1);
});
