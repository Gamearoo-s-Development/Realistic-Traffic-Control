package com.gamearoosdevelopment.realistictrafficcontrol.util;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BaseTrafficLightTileEntity;

public final class LeftTurnBulbHelper {
	private LeftTurnBulbHelper() {}

	public static void setGreen(BaseTrafficLightTileEntity tl, boolean flash) {
		tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft, true, flash);
		tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, flash);
		tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn, true, flash);
		tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, flash);
	}

	public static void setYellow(BaseTrafficLightTileEntity tl, boolean flash) {
		tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, flash);
		tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, flash);
		tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, flash);
		tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn2, true, flash);
	}

	public static void setRed(BaseTrafficLightTileEntity tl, boolean flash) {
		tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, flash);
		tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, flash);
		tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, flash);
		tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, flash);
	}

	public static void clear(BaseTrafficLightTileEntity tl) {
		tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn2, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, false, false);
	}
}
