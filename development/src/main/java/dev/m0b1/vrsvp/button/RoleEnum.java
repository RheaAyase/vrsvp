package dev.m0b1.vrsvp.button;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleEnum {

	HEALER("healer"),
	DEVOTION("devotion"),
	ASURA("asura"),
	BARD("bard"),
	DANCER("dancer"),
	CC("cc"),
	RANGED_DPS("ranged dps"),
	MELEE_DPS("melee dps"),
	LP_DISPEL_SAGE("lp-dispel sage");

	private final String label;

	public static RoleEnum getByLabel(String label) {

		for (RoleEnum roleEnum : RoleEnum.values()) {
			if (roleEnum.getLabel().equals(label)) {
				return roleEnum;
			}
		}

		return null;
	}

}
