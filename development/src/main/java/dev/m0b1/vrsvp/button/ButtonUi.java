package dev.m0b1.vrsvp.button;

import dev.m0b1.vrsvp.util.Parser;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@UtilityClass
public class ButtonUi {

	public static final Emoji EMOJI_RSVP = Emoji.fromUnicode("\uD83D\uDCDD"); // :pencil:
	public static final Emoji EMOJI_EDIT = Emoji.fromUnicode("\uD83D\uDD12"); // :lock:

	/**
	 * Builds a list of buttons shown at the bottom of an RSVP form for anyone to use.
	 *
	 * This is cleaner than just trying to shove all the signup buttons together.
	 */
	public static List<Button> buildRsvpActionPrompts() {
		return List.of(
			Button.primary(ButtonEnum.RSVP.getId(), ButtonEnum.RSVP.getLabel()).withEmoji(EMOJI_RSVP),
			Button.secondary(ButtonEnum.EDIT.getId(), ButtonEnum.EDIT.getLabel()).withEmoji(EMOJI_EDIT)
		);
	}

	/**
	 * Builds buttons that correspond to editing either event description or RSVP button status.
	 */
	public static List<Button> buildEditActionPrompts() {
		return List.of(
			Button.primary(ButtonEnum.EDIT_EVENT_DESCRIPTION.getId(), ButtonEnum.EDIT_EVENT_DESCRIPTION.getLabel()),
			Button.danger(ButtonEnum.EDIT_EVENT_ACTIVE.getId(), ButtonEnum.EDIT_EVENT_ACTIVE.getLabel())
		);
	}

	/**
	 * Builds a list of action rows containing buttons that correspond to available objects in an RSVP message.
	 *
	 * We can only have five buttons per row, and five rows.
	 */
	public static List<ActionRow> buildIndexedButtonActionRows(String actionId, int buttonCount) {

		var buttons = new LinkedList<Button>();

		for (var buttonIndex = 0; buttonIndex < buttonCount; buttonIndex++) {

			var id = "%s:%d".formatted(actionId, buttonIndex);
			var label = "#%d".formatted(buttonIndex + 1);

			buttons.add(Button.primary(id, label));
		}

		return ActionRow.partitionOf(buttons);
	}

	/**
	 * Builds the ephemeral RSVP controls, either buttons to pick a slot or the role dropdown when there is only one.
	 *
	 * Users already signed up for a single slot event (non-null current roles) also get a button to remove their RSVP.
	 */
	public static List<ActionRow> buildRsvpActionRows(int slots, List<RoleEnum> currentRoles) {

		if (slots > 1) {
			return buildIndexedButtonActionRows(ButtonEnum.RSVP.getId(), slots);
		}

		var actionRows = new LinkedList<ActionRow>();
		actionRows.add(buildRoleSelectActionRow(0, currentRoles));

		if (currentRoles != null) {
			var removeId = "%s:0".formatted(ButtonEnum.RSVP.getId());
			actionRows.add(ActionRow.of(Button.danger(removeId, "Remove RSVP")));
		}

		return actionRows;
	}

	/**
	 * Builds a dropdown of roles for a user to pick one or more from when signing up for the given slot.
	 *
	 * Any roles the user already has are pre-selected so they only need to adjust them.
	 */
	public static ActionRow buildRoleSelectActionRow(int slotIndex, List<RoleEnum> currentRoles) {

		var options = Arrays.stream(RoleEnum.values())
			.map(role -> SelectOption.of(role.getLabel(), role.getLabel()))
			.toList();

		var menu = StringSelectMenu.create("%s:%d".formatted(ButtonEnum.RSVP_ROLE.getId(), slotIndex))
			.setPlaceholder(ButtonEnum.RSVP_ROLE.getLabel())
			.setRequiredRange(1, options.size())
			.addOptions(options);

		if (currentRoles != null) {
			menu.setDefaultValues(currentRoles.stream().map(RoleEnum::getLabel).toList());
		}

		return ActionRow.of(menu.build());
	}

	/**
	 * Format how a user's signup appears in a slot, like "@User (healer/bard)".
	 */
	public static String buildSignup(@Nonnull String userMention, @Nonnull List<RoleEnum> roles) {
		return "%s (%s)".formatted(userMention, joinRoles(roles, Parser.ROLE_DELIMITER));
	}

	/**
	 * Join role labels for display, like "healer, bard".
	 */
	public static String joinRoles(@Nonnull List<RoleEnum> roles, @Nonnull String delimiter) {
		return String.join(delimiter, roles.stream().map(RoleEnum::getLabel).toList());
	}

	/**
	 * Remove a user's existing signup from the specified slot and, if given, add their new signup to the end of it.
	 */
	public static String editRsvpInSlot(
		@Nonnull Message message,
		@Nonnull String userMention,
		String signup,
		int slotIndexDestination
	) {

		var slotIndex = 0;

		var description = Parser.readMessageDescription(message);

		var descriptionLines = new LinkedList<>(description.lines().toList());

		if (Parser.countSlotsInText(description) == 0) {
			return replaceUserSignupInList(descriptionLines, userMention, signup);
		}

		for (var lineIndex = 0; lineIndex < descriptionLines.size(); lineIndex++) {

			var line = descriptionLines.get(lineIndex);

			if (Parser.isSlot(line)) {

				if (slotIndex == slotIndexDestination) {
					var editedLine = replaceUserSignupInSlot(line, userMention, signup);
					descriptionLines.set(lineIndex, editedLine);
				}

				slotIndex++;
			}
		}

		return String.join(Parser.SLOT_DELIMITER, descriptionLines);
	}

	private static String replaceUserSignupInList(List<String> descriptionLines, String userMention, String signup) {

		descriptionLines.removeIf(line -> Parser.isSignup(line) && line.contains(userMention));

		if (signup != null) {

			var lastSignupIndex = -1;
			for (var lineIndex = 0; lineIndex < descriptionLines.size(); lineIndex++) {
				if (Parser.isSignup(descriptionLines.get(lineIndex))) {
					lastSignupIndex = lineIndex;
				}
			}

			var signupIndex = lastSignupIndex >= 0 ? lastSignupIndex + 1 : descriptionLines.size();
			descriptionLines.add(signupIndex, Parser.SIGNUP_PREFIX + signup);
		}

		return String.join(Parser.SLOT_DELIMITER, descriptionLines);
	}

	private static String replaceUserSignupInSlot(String input, String userMention, String signup) {

		var signups = Parser.splitSlotText(input);

		signups.removeIf(existingSignup -> existingSignup.contains(userMention));

		if (signup != null) {
			signups.add(signup);
		}

		return String.join(Parser.SIGNUP_DELIMITER, signups);
	}

}
