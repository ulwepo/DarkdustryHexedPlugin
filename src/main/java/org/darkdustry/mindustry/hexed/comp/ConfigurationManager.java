package org.darkdustry.mindustry.hexed.comp;

import arc.files.Fi;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonWriter;
import java.io.*;
import java.nio.file.FileSystemException;
import org.json.JSONObject;

public class ConfigurationManager {

	private Fi jsonFile;
	private Json json;

	public ConfigurationManager() throws IOException {
		init("config/mods/" + getPluginName() + "/");
	}

	public JSONObject getJsonData() {
		JsonValue value = json.fromJson(null, jsonFile);
		return new JSONObject(value.toJson(JsonWriter.OutputType.json));
	}

	private void init(String configPath) throws IOException {
		File configurationFile = new File(configPath, "settings.json");
		if (!configurationFile.getParentFile().exists()) {
			boolean isCreated = configurationFile.getParentFile().mkdirs();
			if (!isCreated) throw new FileSystemException(configPath);
		}

		jsonFile = new Fi(configurationFile);
		json = new Json(JsonWriter.OutputType.json);

		if (!configurationFile.exists()) {
			InputStream in = getClass().getResourceAsStream("/config/" + "settings.json");

			if (in == null) throw new FileNotFoundException(
				"resources:///config/" + "settings.json"
			);

			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			String line;
			while ((line = reader.readLine()) != null) {
				jsonFile.writeString(line + "\n", true);
			}
		}
	}

	private String getPluginName() throws IOException {
		InputStream in = getClass().getResourceAsStream("/plugin.json");
		StringBuilder pluginJson = new StringBuilder();

		if (in == null) throw new FileNotFoundException("resources:///plugin.json");

		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
		while ((line = reader.readLine()) != null) {
			pluginJson.append(line);
		}

		return new JSONObject(pluginJson.toString()).getString("name");
	}

	public void setJsonValue(JSONObject jsonData, String name, int value) {
		jsonData.remove(name);
		jsonData.put(name, value);
	}
}
