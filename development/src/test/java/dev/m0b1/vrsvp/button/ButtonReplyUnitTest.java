package dev.m0b1.vrsvp.button;

import dev.m0b1.vrsvp.TestBase;
import dev.m0b1.vrsvp.command.SlashCommandEnum;
import java.util.Optional;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ButtonReplyUnitTest extends TestBase {

	@InjectMocks private ButtonReply reply;

	@BeforeEach
	void beforeEach() {

		when(user.getName()).thenReturn("@Testing");
		when(user.getAsMention()).thenReturn("@Testing");

		when(button.isDisabled()).thenReturn(false);

		when(buttonInteractionEvent.getUser()).thenReturn(user);
		when(buttonInteractionEvent.getMessage()).thenReturn(message);
		when(buttonInteractionEvent.reply(anyString())).thenReturn(replyCallbackAction);
		when(buttonInteractionEvent.editMessage(anyString())).thenReturn(messageEditCallbackAction);
		when(buttonInteractionEvent.replyModal(any())).thenReturn(modalCallbackAction);

		when(stringSelectInteractionEvent.getUser()).thenReturn(user);
		when(stringSelectInteractionEvent.editMessage(anyString())).thenReturn(messageEditCallbackAction);

		when(messageEditCallbackAction.setComponents(any(MessageTopLevelComponent.class))).thenReturn(messageEditCallbackAction);
		when(messageEditCallbackAction.setComponents(anyCollection())).thenReturn(messageEditCallbackAction);

		when(replyCallbackAction.setEphemeral(true)).thenReturn(replyCallbackAction);
		when(replyCallbackAction.setComponents(any(MessageTopLevelComponent.class))).thenReturn(replyCallbackAction);
		when(replyCallbackAction.setComponents(anyCollection())).thenReturn(replyCallbackAction);

		when(message.getComponentTree()).thenReturn(messageComponentTree);
		when(message.editMessageEmbeds(Collections.emptyList())).thenReturn(messageEditAction);
		when(message.editMessageComponents(any(MessageComponentTree.class))).thenReturn(messageEditAction);
		when(message.editMessageComponents(any(ActionRow.class))).thenReturn(messageEditAction);
		when(message.getEmbeds()).thenReturn(List.of(messageEmbed));

		when(messageComponentTree.find(any(), any())).thenReturn(Optional.of(button));
		when(messageComponentTree.replace(any())).thenReturn(messageComponentTree);

		when(messageEditAction.setContent(any())).thenReturn(messageEditAction);

		when(messageEmbed.getDescription()).thenReturn("> #1");
	}

	@Test
	void editInterestBuildsEphemeralActionPrompts() {

		reply.editInterest(buttonInteractionEvent);

		var expectedReply = """
			Use these buttons to edit the RSVP form.
			When editing descriptions, it is suggested to copy the text into your favorite editor for making changes.""";

		verify(buttonInteractionEvent).reply(expectedReply);
		verify(replyCallbackAction).setEphemeral(true);
		verify(replyCallbackAction).setComponents(any(ActionRow.class));
		verify(replyCallbackAction).queue();
	}

	@Test
	void editToggleRsvpActiveFailsIfButtonNotFoundWhenEditingMessageComponents() {

		when(messageComponentTree.find(any(), any())).thenReturn(Optional.empty());

		reply.editToggleRsvpActive(buttonInteractionEvent, message);

		verify(buttonInteractionEvent).editMessage("Could not toggle RSVP button");
		verify(messageEditCallbackAction).queue();
		verify(message, never()).editMessageComponents(any(ActionRow.class));
	}

	// TODO: We no longer have a clean way to test RSVP toggles
	// Thanks, JDA switching up their entire API!

	@Test
	void editEventDescription() {
		reply.editEventDescription(buttonInteractionEvent, message);
		verify(buttonInteractionEvent).replyModal(any());
		verify(modalCallbackAction).queue();
	}

	@Test
	void rsvpInterestWithSingleSlotPromptsForRole() {

		reply.rsvpInterest(buttonInteractionEvent);

		verify(buttonInteractionEvent).reply("Pick your roles.");
		verify(replyCallbackAction).setEphemeral(true);
		verify(replyCallbackAction).setComponents(anyCollection());
		verify(replyCallbackAction).queue();
	}

	@Test
	void rsvpInterestWithSingleSlotWhenSignedUpOffersRemoval() {

		when(messageEmbed.getDescription()).thenReturn("**Signups**\n> @Testing (bard)");

		reply.rsvpInterest(buttonInteractionEvent);

		verify(buttonInteractionEvent).reply("Change your roles or remove your RSVP.");
		verify(replyCallbackAction).queue();
	}

	@Test
	void rsvpInterestWithMultipleSlots() {

		when(messageEmbed.getDescription()).thenReturn("> #1\n> #2");

		reply.rsvpInterest(buttonInteractionEvent);

		verify(buttonInteractionEvent).reply("Use these buttons to toggle your RSVP for any slot.");
		verify(replyCallbackAction).setEphemeral(true);
		verify(replyCallbackAction).setComponents(anyCollection());
		verify(replyCallbackAction).queue();
	}

	@Test
	void rsvpToggleFailsIfRsvpButtonNotFoundOnSourceMessage() {

		when(messageComponentTree.find(any(), any())).thenReturn(Optional.empty());

		reply.rsvpToggle(buttonInteractionEvent, message, 0);

		verify(buttonInteractionEvent).editMessage("RSVP has been disabled for this event.");
		verify(messageEditCallbackAction).queue();
		verify(message, never()).editMessageEmbeds(any(MessageEmbed.class));
	}

	@Test
	void rsvpToggleFailsIfAdminHasTurnedOffRsvpForEvent() {

		when(button.isDisabled()).thenReturn(true);

		reply.rsvpToggle(buttonInteractionEvent, message, 0);

		verify(buttonInteractionEvent).editMessage("RSVP has been disabled for this event.");
		verify(messageEditCallbackAction).queue();
		verify(message, never()).editMessageEmbeds(any(MessageEmbed.class));
	}

	@Test
	void rsvpToggleFailsIfAddingUserMentionAndRsvpLimitPerSlotExceededWithoutEditingDescription() {

		var rule = "%s: 0".formatted(SlashCommandEnum.RSVP_LIMIT_PER_SLOT.getDescription());
		when(messageEmbed.getDescription()).thenReturn("%s%n> #1%n> #2".formatted(rule));

		reply.rsvpToggle(buttonInteractionEvent, message, 1);

		verify(buttonInteractionEvent).editMessage("Signup limit exceeded, cannot RSVP for slot #2");
		verify(messageEditCallbackAction).queue();
		verify(message, never()).editMessageEmbeds(any(MessageEmbed.class));
	}

	@Test
	void rsvpToggleDoesNotThrowExceptionIfThereAreSlotLimitsSetButNoSlots() {

		var rule = "%s: 1".formatted(SlashCommandEnum.RSVP_LIMIT_PER_SLOT.getDescription());
		when(messageEmbed.getDescription()).thenReturn("%s%n".formatted(rule));

		assertDoesNotThrow(() -> reply.rsvpToggle(buttonInteractionEvent, message, 0));
	}

	@Test
	void rsvpSignupFailsIfSignupWouldPutMessageOverContentLengthLimits() {

		var description = "\n> #1";
		var charactersNeededToFillUpDescription = Message.MAX_CONTENT_LENGTH - description.length();
		description = generateString(charactersNeededToFillUpDescription) + description;
		when(messageEmbed.getDescription()).thenReturn(description);
		when(messageEditAction.setContent(any())).thenThrow(new IllegalArgumentException());

		reply.rsvpSignup(stringSelectInteractionEvent, message, 0, List.of(RoleEnum.HEALER));

		verify(messageEditAction).setContent(any());
		verify(messageEditAction, never()).queue();
		verify(messageEditCallbackAction).queue();
	}

	@Test
	void rsvpToggleSucceedsIfRemovingUserMentionWhenRsvpLimitPerSlotExceeded() {

		var rule = "%s: 0".formatted(SlashCommandEnum.RSVP_LIMIT_PER_SLOT.getDescription());
		when(messageEmbed.getDescription()).thenReturn("%s%n> #1%n> #2, @Testing".formatted(rule));

		reply.rsvpToggle(buttonInteractionEvent, message, 1);

		verify(buttonInteractionEvent).editMessage("RSVP removed for slot #2");
		verify(messageEditCallbackAction).queue();
		verify(message).editMessageEmbeds(Collections.emptyList());
		verify(messageEditAction).setContent(any());
		verify(messageEditAction).queue();
	}

	@Test
	void rsvpToggleWhenNotSignedUpPromptsForRole() {

		reply.rsvpToggle(buttonInteractionEvent, message, 0);

		verify(buttonInteractionEvent).editMessage("Pick your roles");
		verify(messageEditCallbackAction).setComponents(any(MessageTopLevelComponent.class));
		verify(messageEditCallbackAction).queue();
		verify(message, never()).editMessageEmbeds(Collections.emptyList());
	}

	@Test
	void rsvpToggleOnNonExistentSlot() {

		when(messageEmbed.getDescription()).thenReturn(null);

		reply.rsvpToggle(buttonInteractionEvent, message, 1);

		verify(buttonInteractionEvent).editMessage("Slot #2 could not be found");
		verify(messageEditCallbackAction).queue();
		verify(message, never()).editMessageEmbeds(Collections.emptyList());
	}

	@Test
	void rsvpToggleRemoveFromSlotWhenAdditionalInformationAdded() {

		when(messageEmbed.getDescription()).thenReturn("> #1, (added text) @Testing");

		reply.rsvpToggle(buttonInteractionEvent, message, 0);

		verify(buttonInteractionEvent).editMessage("RSVP removed");
		verify(messageEditCallbackAction).queue();
		verify(messageEditAction).setContent("> #1");
		verify(messageEditAction).queue();
	}

	@Test
	void rsvpSignupAddsRoleNextToUserMention() {

		reply.rsvpSignup(stringSelectInteractionEvent, message, 0, List.of(RoleEnum.HEALER));

		verify(stringSelectInteractionEvent).editMessage("RSVP added as healer");
		verify(messageEditCallbackAction).setComponents(anyCollection());
		verify(messageEditCallbackAction).queue();
		verify(message).editMessageEmbeds(Collections.emptyList());
		verify(messageEditAction).setContent("> #1, @Testing (healer)");
		verify(messageEditAction).queue();
	}

	@Test
	void rsvpSignupReplacesExistingRole() {

		when(messageEmbed.getDescription()).thenReturn("> #1, @Testing (bard)");

		reply.rsvpSignup(stringSelectInteractionEvent, message, 0, List.of(RoleEnum.MELEE_DPS));

		verify(stringSelectInteractionEvent).editMessage("RSVP added as melee dps");
		verify(messageEditAction).setContent("> #1, @Testing (melee dps)");
		verify(messageEditAction).queue();
	}

	@Test
	void rsvpSignupFailsIfRsvpLimitPerSlotExceeded() {

		var rule = "%s: 1".formatted(SlashCommandEnum.RSVP_LIMIT_PER_SLOT.getDescription());
		when(messageEmbed.getDescription()).thenReturn("%s%n> #1, @Other (cc)".formatted(rule));

		reply.rsvpSignup(stringSelectInteractionEvent, message, 0, List.of(RoleEnum.HEALER));

		verify(stringSelectInteractionEvent).editMessage("Signup limit exceeded, cannot RSVP");
		verify(messageEditCallbackAction).queue();
		verify(message, never()).editMessageEmbeds(Collections.emptyList());
	}

	@Test
	void rsvpSignupStayingMeetingLimits() {

		var limitSlot = "%s: 1".formatted(SlashCommandEnum.RSVP_LIMIT_PER_SLOT.getDescription());
		when(messageEmbed.getDescription()).thenReturn("%s%n> #1".formatted(limitSlot));

		reply.rsvpSignup(stringSelectInteractionEvent, message, 0, List.of(RoleEnum.BARD));

		verify(stringSelectInteractionEvent).editMessage("RSVP added as bard");
		verify(messageEditCallbackAction).queue();
		verify(message).editMessageEmbeds(Collections.emptyList());
		verify(messageEditAction).setContent(any());
		verify(messageEditAction).queue();
	}

	@Test
	void rsvpSignupStayingUnderLimits() {

		var limitSlot = "%s: 2".formatted(SlashCommandEnum.RSVP_LIMIT_PER_SLOT.getDescription());
		when(messageEmbed.getDescription()).thenReturn("%s%n> #1".formatted(limitSlot));

		reply.rsvpSignup(stringSelectInteractionEvent, message, 0, List.of(RoleEnum.BARD));

		verify(stringSelectInteractionEvent).editMessage("RSVP added as bard");
		verify(messageEditCallbackAction).queue();
		verify(message).editMessageEmbeds(Collections.emptyList());
		verify(messageEditAction).setContent(any());
		verify(messageEditAction).queue();
	}

	@Test
	void rsvpSignupWithMultipleRoles() {

		when(messageEmbed.getDescription()).thenReturn("**Signups**\n> @Testing (bard)");

		reply.rsvpSignup(stringSelectInteractionEvent, message, 0, List.of(RoleEnum.HEALER, RoleEnum.RANGED_DPS));

		verify(stringSelectInteractionEvent).editMessage("RSVP added as healer, ranged dps");
		verify(messageEditAction).setContent("**Signups**\n> @Testing (healer/ranged dps)");
	}

	@Test
	void rsvpToggleRemovesMultipleRoleSignupFromSlotLine() {

		when(messageEmbed.getDescription()).thenReturn("> #1, @Testing (bard/cc), @Other (dancer)\n> #2");

		reply.rsvpToggle(buttonInteractionEvent, message, 0);

		verify(messageEditAction).setContent("> #1, @Other (dancer)\n> #2");
	}

	@Test
	void rsvpSignupForMultipleSlotsNamesTheSlot() {

		when(messageEmbed.getDescription()).thenReturn("> #1\n> #2");

		reply.rsvpSignup(stringSelectInteractionEvent, message, 1, List.of(RoleEnum.CC));

		verify(stringSelectInteractionEvent).editMessage("RSVP added for slot #2 as cc");
		verify(messageEditAction).setContent("> #1\n> #2, @Testing (cc)");
	}

	@Test
	void rsvpSignupListsUsersVerticallyWithoutSlotLines() {

		when(messageEmbed.getDescription()).thenReturn("**Signups**\n> @Other (bard)\n\nFooter");

		reply.rsvpSignup(stringSelectInteractionEvent, message, 0, List.of(RoleEnum.HEALER));

		verify(stringSelectInteractionEvent).editMessage("RSVP added as healer");
		verify(messageEditAction).setContent("**Signups**\n> @Other (bard)\n> @Testing (healer)\n\nFooter");
		verify(messageEditAction).queue();
	}

	@Test
	void rsvpToggleRemovesUserFromVerticalList() {

		when(messageEmbed.getDescription()).thenReturn("**Signups**\n> @Testing (bard)\n> @Other (cc)");

		reply.rsvpToggle(buttonInteractionEvent, message, 0);

		verify(buttonInteractionEvent).editMessage("RSVP removed");
		verify(messageEditAction).setContent("**Signups**\n> @Other (cc)");
	}

	@Test
	void rsvpSignupFailsIfRsvpLimitPerSlotExceededInVerticalList() {

		var rule = "%s: 1".formatted(SlashCommandEnum.RSVP_LIMIT_PER_SLOT.getDescription());
		when(messageEmbed.getDescription()).thenReturn("%s%n**Signups**%n> @Other (cc)".formatted(rule));

		reply.rsvpSignup(stringSelectInteractionEvent, message, 0, List.of(RoleEnum.HEALER));

		verify(stringSelectInteractionEvent).editMessage("Signup limit exceeded, cannot RSVP");
		verify(message, never()).editMessageEmbeds(Collections.emptyList());
	}

	@Test
	void ephemeral() {

		reply.ephemeral(buttonInteractionEvent, "Testing");

		verify(buttonInteractionEvent).reply("Testing");
		verify(replyCallbackAction).setEphemeral(true);
		verify(replyCallbackAction).queue();
	}

}
