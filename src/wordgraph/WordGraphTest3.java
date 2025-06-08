package wordgraph;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

public class WordGraphTest3 {

	@BeforeClass
	public static void setUp() throws IOException {
		WordGraph.buildGraphFromFile("onlyone.txt"); // 使用新的测试文件
	}

	/**
	 * 基本路径3: 起点只有一个节点，无出边，直接结束
	 */
	@Test
	public void testRandomWalk_loopUntilVisitedEdge() {
		String input = "\n\n\n\n\n\n\n\n";
		System.setIn(new ByteArrayInputStream(input.getBytes()));

		String result = WordGraph.randomWalk();
		assertEquals("o", result);
	}
}
