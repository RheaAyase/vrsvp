package dev.m0b1.vrsvp.util;

import dev.m0b1.vrsvp.button.RoleEnum;
import dev.m0b1.vrsvp.command.SlashCommandEnum;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.collections4.CollectionUtils;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@UtilityClass
public class Parser {

	public static final String SIGNUP_DELIMITER = ", ";

	public static final String SLOT_DELIMITER = "\n";

	public static final String SIGNUP_PREFIX = "> ";

	// Not ", " since that already separates signups within a slot line
	public static final String ROLE_DELIMITER = "/";

	private static final Pattern ROLES_PATTERN = Pattern.compile("\\(([^()]*)\\)\\s*$");

	private static final Pattern RSVP_LIMIT_PER_SLOT_PATTERN = buildRegexPatternForLimit(SlashCommandEnum.RSVP_LIMIT_PER_SLOT);

	/**
	 * Read the message description from the first embed if it exists or the content if embeds don't exist.
	 *
	 * Message embeds have been deprecated and will be removed soon.
	 * Defaults to an empty string if not found.
	 */
	public static String readMessageDescription(@Nonnull Message message) {

		var result = message.getContentRaw();
		if (CollectionUtils.isNotEmpty(message.getEmbeds())) {
			var deprecatedEmbed = message.getEmbeds().getFirst();
			result = Objects.requireNonNullElse(deprecatedEmbed.getDescription(), "");
		}

		return result;
	}

	/**
	 * Determines if given line starts with the necessary characters to indicate a slot.
	 */
	public static boolean isSlot(@Nonnull String line) {
		return line.startsWith("> #");
	}

	/**
	 * @see Parser#isSlot for more information.
	 */
	public static int countSlotsInText(@Nonnull String text) {
		return (int) text.lines()
			.filter(Parser::isSlot)
			.count();
	}

	/**
	 * Determines if given line is a signup in an event without slot lines, where signups are listed one per line.
	 */
	public static boolean isSignup(@Nonnull String line) {
		return line.startsWith(SIGNUP_PREFIX) && ! isSlot(line);
	}

	/**
	 * Find the signups for the given slot, or null if that slot does not exist.
	 *
	 * Events without slot lines have a single slot whose signups are listed one per line instead.
	 */
	public static List<String> findSignupsInSlot(@Nonnull String text, int slotIndex) {

		if (countSlotsInText(text) == 0) {
			return slotIndex != 0 ? null : text.lines()
				.filter(Parser::isSignup)
				.map(line -> line.substring(SIGNUP_PREFIX.length()))
				.toList();
		}

		return text.lines()
			.filter(Parser::isSlot)
			.skip(slotIndex)
			.findFirst()
			.map(line -> splitSlotText(line).stream().filter(data -> data.contains("@")).toList())
			.orElse(null);
	}

	/**
	 * Find the known roles listed in parentheses at the end of a signup, like "@User (healer/bard)".
	 */
	public static List<RoleEnum> findRolesInSignup(@Nonnull String signup) {

		var matches = ROLES_PATTERN.matcher(signup);
		if ( ! matches.find()) {
			return List.of();
		}

		return Arrays.stream(matches.group(1).split(ROLE_DELIMITER))
			.map(String::strip)
			.map(RoleEnum::getByLabel)
			.filter(Objects::nonNull)
			.toList();
	}

	/**
	 * Given some slot text, separate them based on known delimiter and convert to a modifiable list.
	 */
	public static List<String> splitSlotText(@Nonnull String text) {
		return new LinkedList<>(Arrays.stream(text.split(SIGNUP_DELIMITER)).toList());
	}

	/**
	 * Find the first result matching the RSVP limit per slot pattern and return the desired integer if found.
	 */
	public static Integer findRsvpLimitPerSlotInText(String text) {
		return runRegexPatternToFindNumberInText(RSVP_LIMIT_PER_SLOT_PATTERN, text);
	}

	private static Pattern buildRegexPatternForLimit(SlashCommandEnum slashCommandEnum) {
		var regex = "%s: (\\d+)".formatted(slashCommandEnum.getDescription());
		return Pattern.compile(regex);
	}

	private static Integer runRegexPatternToFindNumberInText(Pattern pattern, String text) {

		if (text == null) {
			return null;
		}

		var matches = pattern.matcher(text);
		if ( ! matches.find()) {
			return null;
		}

		var result = matches.group(1);
		return Integer.parseInt(result);
	}

}
