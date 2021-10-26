const fs = require('fs');
const { join } = require('path');
const [
    _executable, _jsFile, name = "", description = "",
    author = "", main = "", version = 0, minGameVersion = 0,
    RESOURCES_PATH = join(__dirname, "../src/main/resources")
] = process.argv

fs.mkdirSync(RESOURCES_PATH, { recursive: true })
fs.writeFileSync(
    join(RESOURCES_PATH, "plugin.json"),
    JSON.stringify({
        name,
        displayName: name,
        description,
        author,
        main,
        hidden: true,
        java: true,
        version,
        minGameVersion
    })
)