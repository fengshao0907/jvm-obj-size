package main;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * ����ռ���ֽڴ�С������
 * 
 * @author tinylcy
 *
 */
public class ObjSizeFetcher {

	private static Instrumentation instrumentation;

	public static void premain(String agentArgs, Instrumentation inst) {
		instrumentation = inst;
	}

	/**
	 * ֱ�Ӽ��㵱ǰ����ռ�ÿռ��С��
	 * 
	 * ������ 1. ��ǰ���Լ�����Ļ������������ֶδ�С�� 2. �������������ֶ����ô�С�� 3. ����������������ռ�õĿռ䣻 4.
	 * �������������������ñ���ռ�õĿռ䡣
	 * 
	 * �������� 1. �̳��Գ����Լ���ǰ���������������������ֶ������õĶ���ռ�õĿռ䣻 2. �����������������ÿ��Ԫ�������õĶ���ռ�õĿռ䡣
	 *
	 * @param target
	 * @return
	 */
	public static long sizeOf(Object target) {
		if (instrumentation == null) {
			throw new IllegalStateException("Can't access instrumentation environment.\n"
					+ "Please check jar file containing ObjSizeFetcher class is\n"
					+ "specified in the java's \"-javaagent\" command line argument.");
		}
		return instrumentation.getObjectSize(target);
	}

	/**
	 * �ݹ�������ռ�ÿռ��С��
	 * 
	 * ������ 1. ��ǰ���Լ�����Ļ����������ͺ����ñ���Ĵ�С�� 2. ��ǰ���Լ����������������������õĶ���Ĵ�С��
	 * 
	 * @param target
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static long fullSizeOf(Object target) throws IllegalArgumentException, IllegalAccessException {
		long size = 0;
		Set<Object> visited = new HashSet<Object>();
		Queue<Object> queue = new ArrayDeque<Object>();
		queue.add(target);
		while (!queue.isEmpty()) {
			Object obj = queue.poll();
			// System.out.println("current object class: " + obj.getClass().getName());
			size += skipVisitedElement(visited, obj) ? 0 : sizeOf(obj);
			visited.add(obj);
			Class<?> objClass = obj.getClass();

			if (objClass.isArray()) {
				if (objClass.getName().length() > 2) {
					for (int i = 0, len = Array.getLength(obj); i < len; i++) {
						Object elem = Array.get(obj, i);
						if (elem != null) {
							queue.add(elem);
						}
					}
				}
			} else {
				while (objClass != null) {
					Field[] fields = objClass.getDeclaredFields();
					for (Field field : fields) {
						if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
							continue;
						}
						field.setAccessible(true);
						Object fieldValue = field.get(obj);
						if (fieldValue == null) {
							continue;
						}
						queue.add(fieldValue);
					}
					objClass = objClass.getSuperclass();
				}
			}
		}

		return size;
	}

	private static boolean skipVisitedElement(Set<Object> visited, Object obj) {
		if (obj instanceof String && obj == ((String) obj).intern()) {
			return true;
		}
		return visited.contains(obj);
	}

}
