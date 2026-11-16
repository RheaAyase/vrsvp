package dev.m0b1.vrsvp.button;

import dev.m0b1.vrsvp.util.Mdc;
import dev.m0b1.vrsvp.modal.ModalEnum;
import dev.m0b1.vrsvp.modal.ModalUi;
import dev.m0b1.vrsvp.util.Parser;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.replacer.ComponentReplacer;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class ButtonReply {

	/**
	 * Replies with an ephemeral list of buttons admins can click to start editing part of an RSVP form.
	 */
	public void editInterest(@Nonnull ButtonInteractionEvent event) {

		var buttons = ButtonUi.buildEditActionPrompts();

		var reply = """
			Use these buttons to edit the RSVP form.
			When editing descriptions, it is suggested to copy the text into your favorite editor for making changes.""";

		event.reply(reply)
			.setEphemeral(true)
			.setComponents(ActionRow.of(buttons))
			.queue();
	}

	/**
	 * Toggle the disabled state of the RSVP button on a message.
	 */
	public void editToggleRsvpActive(@Nonnull ButtonInteractionEvent event, @Nonnull Message message) {

		var rsvpButton = findRsvpButtonInMessage(message);
		if (rsvpButton == null) {
			event.editMessage("Could not toggle RSVP button").queue();
			return;
		}

		var replacedMessageComponents = message
			.getComponentTree()
			.replace(ComponentReplacer.of(
				Button.class,
				button -> ButtonEnum.RSVP.getId().equals(button.getCustomId()),
				button -> button.withDisabled( ! button.isDisabled())
			));

		message.editMessageComponents(replacedMessageComponents).queue();

		event.editMessage("RSVP button toggled").queue();
	}

	/**
	 * Launch a modal with one text input to prompt an admin for a complete edit of the event.
	 */
	public void editEventDescription(@Nonnull ButtonInteractionEvent event, @Nonnull Message message) {

		var currentText = Parser.readMessageDescription(message);
		var modal = ModalUi.editText(ModalEnum.EVENT_DESCRIPTION, currentText);

		event.replyModal(modal).queue();
	}

	/**
	 * Replies with an ephemeral list of buttons users can click to toggle RSVP state for slots.
	 *
	 * Events with only one slot skip straight to picking a role for it.
	 */
	public void rsvpInterest(@Nonnull ButtonInteractionEvent event) {

		var description = Parser.readMessageDescription(event.getMessage());
		var slots = Parser.countSlotsInText(description);

		var reply = "Use these buttons to toggle your RSVP for any slot.";
		var signedUp = false;

		if (slots <= 1) {
			var userMention = event.getUser().getAsMention();
			signedUp = isSignedUp(Parser.findSignupsInSlot(description, 0), userMention);
			reply = signedUp ? "Pick a new role or remove your RSVP." : "Pick your role.";
		}

		event.reply(reply)
			.setEphemeral(true)
			.setComponents(ButtonUi.buildRsvpActionRows(slots, signedUp))
			.queue();
	}

	/**
	 * Removes the user from the given slot if they are signed up, otherwise prompts them to pick a role for it.
	 */
	public void rsvpToggle(
		@Nonnull ButtonInteractionEvent event,
		@Nonnull Message message,
		int slotIndex
	) {
		rsvpUpdate(event, message, slotIndex, null);
	}

	/**
	 * Signs the user up for the given slot with their chosen role, replacing any role they already had there.
	 */
	public void rsvpSignup(
		@Nonnull StringSelectInteractionEvent event,
		@Nonnull Message message,
		int slotIndex,
		@Nonnull RoleEnum role
	) {
		rsvpUpdate(event, message, slotIndex, role);
	}

	/**
	 * Generic ephemeral reply in response to a button press, mostly for validation errors or development feedback.
	 */
	public void ephemeral(@Nonnull GenericComponentInteractionCreateEvent event, String message) {
		event.reply(message).setEphemeral(true).queue();
	}

	/**
	 * Adjusts RSVP state for given user and slot and then adjusts source ephemeral message to reflect the change.
	 *
	 * Without a role, a user already in the slot is removed and anyone else is asked to pick a role first.
	 */
	private void rsvpUpdate(
		@Nonnull GenericComponentInteractionCreateEvent event,
		@Nonnull Message message,
		int slotIndex,
		RoleEnum role
	) {

		Mdc.put(event);

		var rsvpButton = findRsvpButtonInMessage(message);
		if (rsvpButton == null || rsvpButton.isDisabled()) {

			log.warn("RSVP declined for disabled event");

			event.editMessage("RSVP has been disabled for this event.").queue();
			return;
		}

		var description = Parser.readMessageDescription(message);
		var signups = Parser.findSignupsInSlot(description, slotIndex);
		if (signups == null) {
			event.editMessage("Slot #%d could not be found".formatted(slotIndex + 1)).queue();
			return;
		}

		// Only name the slot when there are several to choose from
		var slots = Parser.countSlotsInText(description);
		var forSlot = slots > 1 ? " for slot #%d".formatted(slotIndex + 1) : "";

		var userMention = event.getUser().getAsMention();
		var signedUp = isSignedUp(signups, userMention);

		if ( ! signedUp && rsvpLimitPerSlotExceeded(description, signups)) {

			log.info("RSVP declined for signup limit");

			event.editMessage("Signup limit exceeded, cannot RSVP%s".formatted(forSlot)).queue();
			return;
		}

		if ( ! signedUp && role == null) {
			event.editMessage("Pick your role%s".formatted(forSlot))
				.setComponents(ButtonUi.buildRoleSelectActionRow(slotIndex))
				.queue();
			return;
		}

		var signup = role == null ? null : ButtonUi.buildSignup(userMention, role);
		var editedDescription = ButtonUi.editRsvpInSlot(message, userMention, signup, slotIndex);

		try {

			message.editMessageEmbeds(Collections.emptyList())
				.setContent(editedDescription)
				.queue();

			var reply = role == null
				? "RSVP removed%s".formatted(forSlot)
				: "RSVP added%s as %s".formatted(forSlot, role.getLabel());

			event.editMessage(reply).setComponents(ButtonUi.buildRsvpActionRows(slots, role != null)).queue();

		} catch (IllegalArgumentException _) {

			var reply = """
				VRSVP has hit the Discord message length limit when trying to RSVP.
				Please contact the event runner to see if they can free up some space in the event description.
				""";

			event.editMessage(reply).queue();
		}
	}

	private boolean rsvpLimitPerSlotExceeded(@Nonnull String description, @Nonnull List<String> signups) {
		var limit = Parser.findRsvpLimitPerSlotInText(description);
		return limit != null && signups.size() >= limit;
	}

	private boolean isSignedUp(@Nonnull List<String> signups, @Nonnull String userMention) {
		return signups.stream().anyMatch(signup -> signup.contains(userMention));
	}

	private Button findRsvpButtonInMessage(@Nonnull Message message) {
		return message
			.getComponentTree()
			.find(Button.class, button -> ButtonEnum.RSVP.getId().equals(button.getCustomId()))
			.orElse(null);
	}

}
