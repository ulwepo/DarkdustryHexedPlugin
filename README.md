<div align="center">
    <h1>Hexed Plugin</h1>
    <p>Мини режим для серверов.</p>
</div>

<br>

## Contributing

Все необходимые правила и советы расписаны в [CONTRIBUTING](https://github.com/Darkdustry-Coders/DarkdustryPlugin/blob/master/CONTRIBUTING.md).

## Компиляция

Gradle может потребоваться до нескольких минут для загрузки файлов. <br>
После сборки выходной .JAR-файл должен находиться в каталоге `/build/libs/DarkdustryPlugin.jar`.

Сначала убедитесь, что у вас установлен JDK 18. Откройте терминал в каталоге проекта и выполните следующие команды:

### Windows

_Компиляция:_ `gradlew jar`

### Linux/Mac OS

_Компиляция:_ `./gradlew jar`

### Устранение неполадок

#### Permission Denied

Если терминал выдает `Permission denied` или `Command not found` на Mac/Linux, выполните `chmod +x ./gradlew` перед запуском `./gradlew`. *Это одноразовая процедура.*
