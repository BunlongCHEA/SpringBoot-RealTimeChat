package com.project.realtimechat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseDTO<T> {
	int statusCode;
	String message;
	T data;
}
