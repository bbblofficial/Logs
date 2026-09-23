package ir.muvixo.logs.velocity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Color utility: legacy (&a, &c) + hex (&#RRGGBB).
 *
 * @author muvixo
 */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ColorUtil() {}

    public static Component color(String text) {
        if (text == null) return Component.empty();
        String withHex = translateHex(text);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(withHex);
    }

    private static String translateHex(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("\u00a7x");
            for (char c : hex.toCharArray()) {
                replacement.append("\u00a7").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static String strip(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)[&\u00a7][0-9A-FK-ORX]", "")
                   .replaceAll("&#[A-Fa-f0-9]{6}", "");
    }
}
