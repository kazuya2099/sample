package com.example.generics;

import java.util.ArrayList;
import java.util.List;

public class Main<T> {

	public static void main(String[] args) {
		Main<Integer> main = new Main<>();
		List<Integer> list = new ArrayList<>();
		list.add(1);
		list.add(2);
		list.add(3);
		main.displayList(list);
	}
	
	private void displayList(List<T> list) {
		Inner inner = new Inner();
		inner.display(list);
	}
	
	/*
	 * 内部クラス。
	 * 外側のクラスのインスタンスにアクセスできる。
	 */
	class Inner {
		void display(List<T> list) {
			list.forEach(System.out::println);
		}
	}
	
	/*
	 * ネストクラス
	 * 名前が「外側のクラス名. ネストしたクラス名」で扱われるが、トップレベルクラスと同等。
	 * 外側クラスのスコープにはアクセスできない。
	 */
	static class Nest1 {
		public Nest1() {
//			List<T> list = new ArrayList<>();
		}
	}
	
	interface Nest2 {
//		void display(List<T> list);
	}
	
	enum Nest3 {
		A, B, C;
		private void display() {
//			T t;
//			System.out.println(t);
		}
	}
}
