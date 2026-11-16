package dev.m0b1.vrsvp.command;

import dev.m0b1.vrsvp.TestBase;
import dev.m0b1.vrsvp.modal.ModalEnum;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlashCommandReplyUnitTest extends TestBase {

	@InjectMocks private SlashCommandReply reply;

	private SlashCommandInputs inputs;

	@BeforeEach
	void beforeEach() {

		inputs = new SlashCommandInputs();

		when(slashCommandInteractionEvent.replyModal(any())).thenReturn(modalCallbackAction);
	}

	@Test
	void rsvpCreationReturnsModalToCheckInputs() {

		inputs.setStartTimestamp(1);

		reply.rsvpCreation(slashCommandInteractionEvent, inputs);

		verify(slashCommandInteractionEvent).replyModal(modalArgumentCaptor.capture());
		verify(modalCallbackAction).queue();

		var label = getLabelComponentFromModal();
		var textInput = getTextInputComponentFromModal();
		Assertions.assertEquals(ModalEnum.EVENT_CREATION.getId(), textInput.getCustomId());
		assertEquals(ModalEnum.EVENT_CREATION.getLabel(), label.getLabel());
		assertEquals(ModalEnum.EVENT_CREATION.getPlaceholder(), textInput.getPlaceHolder());
		assertEquals(TextInputStyle.PARAGRAPH, textInput.getStyle());
	}

	@Test
	void rsvpCreationWithoutRsvpLimitAddendum() {

		inputs.setStartTimestamp(10);

		reply.rsvpCreation(slashCommandInteractionEvent, inputs);

		verify(slashCommandInteractionEvent).replyModal(modalArgumentCaptor.capture());

		var textInput = getTextInputComponentFromModal();

		var expectation = """
			**New Event**

			- Starts <t:10:R> on <t:10:F>

			**Signups**""";

		assertEquals(expectation, textInput.getValue());
	}

	@Test
	void rsvpCreationWithRsvpLimitAddendum() {

		inputs.setStartTimestamp(100);
		inputs.setRsvpLimitPerSlot(2);

		reply.rsvpCreation(slashCommandInteractionEvent, inputs);

		verify(slashCommandInteractionEvent).replyModal(modalArgumentCaptor.capture());

		var textInput = getTextInputComponentFromModal();

		var expectation = """
			**New Event**

			- Starts <t:100:R> on <t:100:F>
			- Maximum number of people that can RSVP: 2

			**Signups**""";

		assertEquals(expectation, textInput.getValue());
	}

	private Label getLabelComponentFromModal() {
		return modalArgumentCaptor
			.getValue()
			.getComponents()
			.getFirst()
			.asLabel();
	}

	private TextInput getTextInputComponentFromModal() {
		return getLabelComponentFromModal()
			.getChild()
			.asTextInput();
	}

}
