package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLClass;

public class MoveClassRefactoring implements Refactoring {
	private UMLClass originalClass;
	private UMLClass movedClass;
	
	public UMLClass getOriginalClass() {
		return originalClass;
	}

	public UMLClass getMovedClass() {
		return movedClass;
	}

	public MoveClassRefactoring(UMLClass originalClass,  UMLClass movedClass) {
		this.originalClass = originalClass;
		this.movedClass = movedClass;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalClass);
		sb.append(" moved to ");
		sb.append(movedClass);
		return sb.toString();
	}

	public RenamePattern getRenamePattern() {
		int separatorPos = separatorPosOfCommonSuffix('.', originalClass.getName(), movedClass.getName());
		String originalPath = originalClass.getName().substring(0, originalClass.getName().length() - separatorPos);
		String movedPath = movedClass.getName().substring(0, movedClass.getName().length() - separatorPos);
		return new RenamePattern(originalPath, movedPath, originalClass, movedClass);
	}

	private int separatorPosOfCommonSuffix(char separator, String s1, String s2) {
		int l1 = s1.length();
		int l2 = s2.length();
		int separatorPos = -1; 
		int lmin = Math.min(s1.length(), s2.length());
		boolean equal = true;
		for (int i = 0; i < lmin; i++) {
			char c1 = s1.charAt(l1 - i - 1);
			char c2 = s2.charAt(l2 - i - 1);
			equal = equal && c1 == c2;
			if (equal && c1 == separator) {
				separatorPos = i;
			}
		}
		return separatorPos;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_CLASS;
	}

	public String getOriginalClassName() {
		return originalClass.getName();
	}

	public String getMovedClassName() {
		return movedClass.getName();
	}

	@Override
	public LocationInfo getBeforeLocationInfo() {
		return originalClass.getLocationInfo();
	}

	@Override
	public LocationInfo getAfterLocationInfo() {
		return movedClass.getLocationInfo();		
	}
	
}
