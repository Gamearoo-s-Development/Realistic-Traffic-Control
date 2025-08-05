package com.gamearoosdevelopment.realistictrafficcontrol.util;

public enum EnumTrafficLightBulbTypes {
	Red(0),
	Yellow(1),
	Green(2),
	RedArrowLeft(3),
	YellowArrowLeft(4),
	GreenArrowLeft(5),
	Cross(6),
	DontCross(7),
	RedArrowRight(8),
	YellowArrowRight(9),
	GreenArrowRight(10),
	NoRightTurn(11),
	NoLeftTurn(12),
	StraightRed(13),
	StraightYellow(14),
	StraightGreen(15),
	RedArrowUTurn(16),
	YellowArrowUTurn(17),
	GreenArrowUTurn(18),
	YellowArrowLeft2(19),
	YellowArrowRight2(20),
	YellowArrowUTurn2(21),
	Red2(22),
	RedX(23),
	GreenDownArrow(24),
	RedArrowRight2(25),
	RedArrowLeft2(26),
	RedArrowUTurn2(27),
	YellowArrowLeft3(28),
	YellowArrowRight3(29),
	YellowArrowUTurn3(30),
	GreenArrowLeft2(31),
	GreenArrowRight2(32),
	GreenArrowUTurn2(33),
	YellowX(34);
	
	
	
	private int index = -1;
	private EnumTrafficLightBulbTypes(int index)
	{
		this.index = index;
	}
	
	public int getIndex()
	{
		return index;
	}
	
	public static EnumTrafficLightBulbTypes get(int index)
	{
		for(EnumTrafficLightBulbTypes bulbType : EnumTrafficLightBulbTypes.values())
		{
			if (bulbType.index == index)
			{
				return bulbType;
			}
		}
		
		return null;
	}
}
