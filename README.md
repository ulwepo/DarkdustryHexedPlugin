# HexedPlugin 

## TODO:
- [x] Локализовать лидерборд
- [ ] Сделать команду /lb (топ-10 игроков сервера)
- [ ] Сохранение никнеймов игроков в джсон вместе с рейтингом (для сайта)

## Building
First, make sure you have JDK 14 installed. Then, setup [plugin.json](src/main/resources/plugin.json) and run the following commands:

* Windows: `gradlew jar`
* *nix/Mac OS: `./gradlew jar`

### Troubleshooting

* If the terminal returns `Permission denied` or `Command not found`, run `chmod +x ./gradlew`.
