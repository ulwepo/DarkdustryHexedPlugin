package ml.darkdustry.hexed.components;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import ml.darkdustry.hexed.HexedMain;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import static mindustry.Vars.mods;

public class Bundles {
    private static final String defaultLanguage = "en";

    private static final Locale defaultLocale = new Locale(defaultLanguage);
    private static final Seq<Locale> supportedLocales = new Seq<>();

    private static final ObjectMap<Locale, ResourceBundle> bundles = new ObjectMap<>();
    private static final ObjectMap<Locale, MessageFormat> formats = new ObjectMap<>();

    public static void init() {
        var files = getBundles().seq();

        files.each(file -> {
            var code = file.nameWithoutExtension().split("_");
            if (code[1].equals(defaultLanguage)) {
                supportedLocales.add(Locale.ROOT);
            } else {
                if (code.length == 3) {
                    supportedLocales.add(new Locale(code[1], code[2]));
                } else {
                    supportedLocales.add(new Locale(code[1]));
                }
            }
        });

        supportedLocales.each(locale -> {
            bundles.put(locale, ResourceBundle.getBundle("bundle.bundle", locale));
            formats.put(locale, new MessageFormat("", locale));
        });

        HexedMain.info("Supported locales: @. Default locale: @.", supportedLocales.size, defaultLocale);
    }

    private static Fi getBundles() {
        return mods.getMod("hexed-plugin").root.child("bundles");
    }
}
