package com.sipm.user.dto;

/** 사용자 등록/수정에서 공통으로 쓰는 입력 형식 (빈 값은 허용 = 선택 입력) */
public final class UserFieldRules {
    /** 영어 이름: 영문자로 시작, 영문자·공백·.'- 만 (예: Gil-dong Hong) */
    public static final String ENGLISH_NAME = "^$|^[A-Za-z][A-Za-z .'-]*$";
    public static final String ENGLISH_NAME_MESSAGE = "영어 이름은 영문자로 시작하고 영문자, 공백, . ' - 만 쓸 수 있습니다.";

    /** 전화번호: 0으로 시작, 하이픈은 있어도 없어도 됨 (예: 010-1234-5678, 02-123-4567) */
    public static final String PHONE = "^$|^0[0-9]{1,2}-?[0-9]{3,4}-?[0-9]{4}$";
    public static final String PHONE_MESSAGE = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)";

    private UserFieldRules() {
    }
}
