package com.mahghuuuls.ashenskills.config;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.weapon.TypeRules;
import com.mahghuuuls.ashenskills.weapon.TypeRules.ItemRule;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.ResourceLocation;

/**
 * The server's pack configuration as a client needs it: the values of every skill and the pack's
 * weapon type rules. A client on someone else's server uses this in place of its own pack files.
 */
public final class SettingsView {

    private final Map<String, SkillValues> skills;
    private final TypeRules typeRules;

    private SettingsView(Map<String, SkillValues> skills, TypeRules typeRules) {
        this.skills = skills;
        this.typeRules = typeRules;
    }

    /** @param effective the skills with the server's values in place */
    public static SettingsView of(Collection<SkillDefinition> effective, TypeRules typeRules) {
        Map<String, SkillValues> skills = new LinkedHashMap<>();
        for (SkillDefinition skill : effective) {
            skills.put(skill.id().toString(), SkillValues.of(skill));
        }
        return new SettingsView(skills, typeRules);
    }

    public TypeRules typeRules() {
        return typeRules;
    }

    /**
     * This client's skills with the server's values. A value the server did not send (it runs
     * another version of the mod) and a skill the server does not have keep what the client's mods
     * registered; such a skill cannot be used on that server anyway.
     *
     * @param registered the skills as the mods on this client registered them, by id
     */
    public Map<ResourceLocation, SkillDefinition> appliedTo(Map<ResourceLocation, SkillDefinition> registered) {
        Map<ResourceLocation, SkillDefinition> result = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, SkillDefinition> skill : registered.entrySet()) {
            SkillValues values = skills.get(skill.getKey().toString());
            result.put(skill.getKey(),
                    values == null ? skill.getValue() : values.over(SkillValues.of(skill.getValue())).appliedTo(skill.getValue()));
        }
        return result;
    }

    public void write(ByteBuf buf) {
        buf.writeShort(skills.size());
        for (Map.Entry<String, SkillValues> skill : skills.entrySet()) {
            writeText(buf, skill.getKey());
            SkillValues values = skill.getValue();
            buf.writeByte(values.numbers.size());
            for (Map.Entry<SkillNumber, Double> number : values.numbers.entrySet()) {
                writeText(buf, number.getKey().configName());
                buf.writeDouble(number.getValue());
            }
            buf.writeByte(values.flags.size());
            for (Map.Entry<SkillFlag, Boolean> flag : values.flags.entrySet()) {
                writeText(buf, flag.getKey().configName());
                buf.writeBoolean(flag.getValue());
            }
            writeTexts(buf, values.weaponTypes);
            writeTexts(buf, values.allowedStates);
            buf.writeByte(values.hitWindowSeconds.size());
            for (double window : values.hitWindowSeconds) {
                buf.writeDouble(window);
            }
        }
        writeTexts(buf, typeRules.packTypes());
        buf.writeShort(typeRules.items().size());
        for (Map.Entry<String, ItemRule> item : typeRules.items().entrySet()) {
            writeText(buf, item.getKey());
            buf.writeBoolean(item.getValue().replaces());
            writeTexts(buf, item.getValue().types());
        }
    }

    /** Values under a name this client does not know (a newer server) are skipped. */
    public static SettingsView read(ByteBuf buf) {
        Map<String, SkillValues> skills = new LinkedHashMap<>();
        for (int skill = buf.readUnsignedShort(); skill > 0; skill--) {
            String id = readText(buf);
            SkillValues values = new SkillValues();
            for (int n = buf.readUnsignedByte(); n > 0; n--) {
                String name = readText(buf);
                double value = buf.readDouble();
                for (SkillNumber key : SkillNumber.values()) {
                    if (key.configName().equals(name)) {
                        values.numbers.put(key, value);
                    }
                }
            }
            for (int n = buf.readUnsignedByte(); n > 0; n--) {
                String name = readText(buf);
                boolean value = buf.readBoolean();
                for (SkillFlag key : SkillFlag.values()) {
                    if (key.configName().equals(name)) {
                        values.flags.put(key, value);
                    }
                }
            }
            values.weaponTypes.addAll(readTexts(buf));
            values.allowedStates.addAll(readTexts(buf));
            for (int n = buf.readUnsignedByte(); n > 0; n--) {
                values.hitWindowSeconds.add(buf.readDouble());
            }
            skills.put(id, values);
        }
        Set<String> packTypes = readTexts(buf);
        Map<String, ItemRule> items = new LinkedHashMap<>();
        for (int item = buf.readUnsignedShort(); item > 0; item--) {
            String name = readText(buf);
            boolean replace = buf.readBoolean();
            items.put(name, new ItemRule(replace, readTexts(buf)));
        }
        return new SettingsView(skills, new TypeRules(packTypes, items));
    }

    private static void writeTexts(ByteBuf buf, Collection<String> texts) {
        buf.writeShort(texts.size());
        for (String text : texts) {
            writeText(buf, text);
        }
    }

    private static Set<String> readTexts(ByteBuf buf) {
        Set<String> texts = new LinkedHashSet<>();
        for (int n = buf.readUnsignedShort(); n > 0; n--) {
            texts.add(readText(buf));
        }
        return texts;
    }

    private static void writeText(ByteBuf buf, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }

    private static String readText(ByteBuf buf) {
        byte[] bytes = new byte[buf.readUnsignedShort()];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
