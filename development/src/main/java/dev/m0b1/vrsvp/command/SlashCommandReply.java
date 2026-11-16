package dev.m0b1.vrsvp.command;

import dev.m0b1.vrsvp.modal.ModalEnum;
import dev.m0b1.vrsvp.modal.ModalUi;
import dev.m0b1.vrsvp.util.Parser;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import java.util.LinkedList;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class SlashCommandReply {

	/**
	 * Build a modal with generated event title and slots for admins to confirm/edit before sending message to channel.
	 */
	public void rsvpCreation(@Nonnull SlashCommandInteractionEvent event, @Nonnull SlashCommandInputs inputs) {

		var description = buildDescription(inputs);
		var modal = ModalUi.editText(ModalEnum.EVENT_CREATION, description);

		event.replyModal(modal).queue();
	}

	/**
	 * Build an RSVP form everyone can see with a single slot, whose signups get listed one per line under a heading.
	 */
	private String buildDescription(@Nonnull SlashCommandInputs inputs) {

		var startTimestamp = inputs.getStartTimestamp();

		var description = new LinkedList<String>();
		description.add("**New Event**\n");
		description.add("- Starts <t:%d:R> on <t:%d:F>".formatted(startTimestamp, startTimestamp));
		description.add(buildRsvpLimitAddendum(SlashCommandEnum.RSVP_LIMIT_PER_SLOT, inputs.getRsvpLimitPerSlot()));
		description.add("");
		description.add("**Signups**");

		description.removeIf(Objects::isNull);

		return String.join(Parser.SLOT_DELIMITER, description);
	}

	private String buildRsvpLimitAddendum(SlashCommandEnum slashCommandEnum, Integer limit) {

		if (limit != null) {
			return "- %s: %d".formatted(slashCommandEnum.getDescription(), limit);
		}

		return null;
	}

}
