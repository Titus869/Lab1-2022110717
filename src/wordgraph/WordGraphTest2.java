package wordgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

public class WordGraphTest2 {

	@BeforeClass
	public static void setUp() throws IOException {
		WordGraph.buildGraphFromFile("test.txt");
	}

	/**
	 * 基本路径1: 图为空，直接返回提示
	 */
	@Test
	public void testRandomWalk_emptyGraph() throws Exception {
		Field graphField = WordGraph.class.getDeclaredField("graph");
		graphField.setAccessible(true);
		Map<String, Map<String, Integer>> graph = (Map<String, Map<String, Integer>>) graphField.get(null);
		Map<String, Map<String, Integer>> backup = new HashMap<>(graph); // 备份

		graph.clear();
		String result = WordGraph.randomWalk();
		assertEquals("Graph is empty.", result);

		graph.putAll(backup); // 恢复图
	}

	/**
	 * 基本路径2: 多次跳转直到重复边，模拟用户多次回车
	 */
	@Test
	public void testRandomWalk_loopUntilVisitedEdge() {
		String input = "\n\n\n\n\n\n\n\n";
		System.setIn(new ByteArrayInputStream(input.getBytes()));

		String result = WordGraph.randomWalk();
		assertTrue(result.contains("->"));
	}

	/**
	 * 基本路径4: 走几步后用户主动终止
	 */
	@Test
	public void testRandomWalk_userTerminates() {
		String input = "\n\nq\n";
		System.setIn(new ByteArrayInputStream(input.getBytes()));

		String result = WordGraph.randomWalk();
		assertTrue(result.split("->").length >= 2);
	}

}
