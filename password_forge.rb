#!/usr/bin/env ruby
# password_forge.rb
# encoding: UTF-8

require 'optparse'
require 'securerandom'
require 'set'

# ANSI colors
COLORS = {
  green: "\e[92m",
  yellow: "\e[93m",
  red: "\e[91m",
  blue: "\e[94m",
  reset: "\e[0m"
}

def colorize(text, color)
  "#{COLORS[color]}#{text}#{COLORS[:reset]}"
end

# Встроенный словарь
COMMON_PASSWORDS = %w[
  password 123456 12345678 1234 qwerty 12345 dragon baseball football letmein
  monkey abc123 mustang michael shadow master jennifer 111111 2000 jordan
  superman harley 1234567 fuckyou hunter trustno1 ranger buster thomas tigger
  robert soccer battle killer hockey george andrew charlie donald asshole
  qwertyuiop liverpool nicole daniel ginger whatever samantha jasmine passw0rd qazwsx
].to_set

def is_common?(pwd)
  COMMON_PASSWORDS.include?(pwd.downcase)
end

def estimate_entropy(pwd, charset_size = nil)
  if charset_size.nil?
    charset_size = pwd.chars.to_set.size
  end
  return 0 if charset_size <= 1
  pwd.length * Math.log2(charset_size)
end

def format_time(seconds)
  return "∞ (слишком долго)" if seconds.infinite?
  if seconds < 1
    "#{seconds.round(3)} секунд"
  elsif seconds < 60
    "#{seconds.round} секунд"
  elsif seconds < 3600
    "#{(seconds/60).round} минут"
  elsif seconds < 86400
    "#{(seconds/3600).round} часов"
  elsif seconds < 31536000
    "#{(seconds/86400).round} дней"
  else
    "#{(seconds/31536000).round} лет"
  end
end

def generate_pronounceable(length)
  vowels = 'aeiouy'
  consonants = 'bcdfghjklmnpqrstvwxz'
  pwd = (0...length).map do |i|
    if i.even?
      consonants[rand(consonants.length)]
    else
      vowels[rand(vowels.length)]
    end
  end.join
  if rand < 0.5 && length > 0
    pwd[0] = pwd[0].upcase
  end
  pwd
end

def generate_password(length, use_upper, use_lower, use_digits, use_special,
                      exclude_ambiguous, pronounceable)
  if pronounceable
    return generate_pronounceable(length)
  end

  chars = ''
  chars << 'abcdefghijklmnopqrstuvwxyz' if use_lower
  chars << 'ABCDEFGHIJKLMNOPQRSTUVWXYZ' if use_upper
  chars << '0123456789' if use_digits
  chars << '!\"#$%&\'()*+,-./:;<=>?@[\\]^_`{|}~' if use_special
  raise "Ни один набор символов не выбран" if chars.empty?

  if exclude_ambiguous
    ambiguous = 'O0o1lI'
    chars = chars.chars.reject { |c| ambiguous.include?(c) }.join
  end

  (0...length).map { chars[rand(chars.length)] }.join
end

options = {
  length: 16,
  count: 1,
  uppercase: true,
  lowercase: true,
  digits: true,
  special: true,
  exclude: false,
  pronounce: false,
  min_entropy: 0,
  quiet: false,
  output: nil,
  help: false
}

OptionParser.new do |opts|
  opts.banner = "Usage: password_forge.rb [options]"
  opts.on('-l', '--length N', Integer, "Длина пароля (default 16)") { |v| options[:length] = v }
  opts.on('-c', '--count N', Integer, "Количество паролей (default 1)") { |v| options[:count] = v }
  opts.on('-u', '--uppercase', "Включать заглавные (default on)") { |v| options[:uppercase] = true }
  opts.on('-L', '--lowercase', "Включать строчные (default on)") { |v| options[:lowercase] = true }
  opts.on('-d', '--digits', "Включать цифры (default on)") { |v| options[:digits] = true }
  opts.on('-s', '--special', "Включать спецсимволы (default on)") { |v| options[:special] = true }
  opts.on('-e', '--exclude', "Исключать неоднозначные символы") { options[:exclude] = true }
  opts.on('-p', '--pronounce', "Генерировать произносимые пароли") { options[:pronounce] = true }
  opts.on('-m', '--min-entropy N', Float, "Минимальная энтропия (бит)") { |v| options[:min_entropy] = v }
  opts.on('-q', '--quiet', "Не выводить оценку сложности") { options[:quiet] = true }
  opts.on('-o', '--output FILE', "Сохранить в файл") { |v| options[:output] = v }
  opts.on('-h', '--help', "Справка") { options[:help] = true }
end.parse!

if options[:help]
  puts <<~HELP
    Usage: password_forge.rb [options]
    Options:
      -l, --length N       Длина пароля (default 16)
      -c, --count N        Количество паролей (default 1)
      -u, --uppercase      Включать заглавные (default on)
      -L, --lowercase      Включать строчные (default on)
      -d, --digits         Включать цифры (default on)
      -s, --special        Включать спецсимволы (default on)
      -e, --exclude        Исключать неоднозначные символы
      -p, --pronounce      Генерировать произносимые пароли
      -m, --min-entropy N  Минимальная энтропия (бит)
      -q, --quiet          Не выводить оценку сложности
      -o, --output FILE    Сохранить в файл
      -h, --help           Справка
  HELP
  exit
end

passwords = []
options[:count].times do
  attempts = 0
  loop do
    pwd = generate_password(
      options[:length],
      options[:uppercase],
      options[:lowercase],
      options[:digits],
      options[:special],
      options[:exclude],
      options[:pronounce]
    )
    attempts += 1
    raise "Не удалось сгенерировать пароль с нужной энтропией" if attempts > 1000
    if estimate_entropy(pwd) >= options[:min_entropy]
      passwords << pwd
      break
    end
  end
end

output = ''
passwords.each do |pwd|
  entropy = estimate_entropy(pwd)
  common = is_common?(pwd)
  crack_sec = entropy.zero? ? Float::INFINITY : (2 ** entropy) / 1e9
  crack_str = format_time(crack_sec)
  strength = if entropy < 30
               colorize("Слабый", :red)
             elsif entropy < 50
               colorize("Средний", :yellow)
             else
               colorize("Сильный", :green)
             end

  output << "Пароль: #{pwd}\n"
  unless options[:quiet]
    output << "  Энтропия: #{entropy.round} бит, Стойкость: #{strength}"
    output << ", Время взлома: #{crack_str}"
    output << colorize(" (Словарный!)", :red) if common
    output << "\n\n"
  else
    output << "\n"
  end
end

if options[:output]
  begin
    File.write(options[:output], output, encoding: 'UTF-8')
    puts colorize("Результат сохранён в #{options[:output]}", :green)
  rescue => e
    puts colorize("Ошибка записи файла: #{e.message}", :red)
    exit 1
  end
else
  print output
end
