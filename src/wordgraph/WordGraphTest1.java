package wordgraph;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

public class WordGraphTest1 {

	/**
	 * 构建图，数据来源 test.txt: a c b x d a k b
	 */
	@BeforeClass
	public static void setUp() throws IOException {
		WordGraph.buildGraphFromFile("test.txt");
		WordGraph.showDirectedGraph();
	}

	/**
	 * 有效等价类：起点终点都存在，存在多个桥接词 桥接词：c, k （顺序为字典序排序）
	 */
	@Test
	public void testMultipleBridgeWords() {
		String res = WordGraph.queryBridgeWords("a", "b");
		assertEquals("The bridge words from a to b are: c and k.", res);
	}

	/**
	 * 有效等价类：起点终点都存在，存在一个桥接词 桥接词：x
	 */
	@Test
	public void testSingleBridgeWord() {
		String res = WordGraph.queryBridgeWords("b", "d");
		assertEquals("The bridge words from b to d are: x.", res);
	}

	/**
	 * 有效等价类：起点终点都存在，但没有桥接词
	 */
	@Test
	public void testNoBridgeWords_xToB() {
		String res = WordGraph.queryBridgeWords("x", "b");
		assertEquals("No bridge words from x to b!", res);
	}

	/**
	 * 无效等价类：两个单词都不在图中
	 */
	@Test
	public void testBothWordsNotExist() {
		String res = WordGraph.queryBridgeWords("foo", "bar");
		assertEquals("No foo or bar in the graph!", res);
	}

	/**
	 * 无效等价类：第一个单词存在，第二个单词不存在
	 */
	@Test
	public void testSecondWordNotExist() {
		String res = WordGraph.queryBridgeWords("a", "z");
		assertEquals("No a or z in the graph!", res);
	}

	/**
	 * 无效等价类：第一个单词存在，第二个单词不存在
	 */
	@Test
	public void testFirstWordNotExist() {
		String res = WordGraph.queryBridgeWords("z", "a");
		assertEquals("No z or a in the graph!", res);
	}

}
