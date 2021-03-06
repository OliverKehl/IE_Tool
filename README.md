#基于信息抽取(IE)的车商批量资源发布工具  

自然语言文本中抽取指定类型的实体、关系、事件等事实信息，并形成结构化数据输出的文本处理技术  

车商的朋友圈信息算是领域相关的自然语言，处理难度比较小(对底层NLP算法的依赖较小，例如分词、tagging、parsing等)  

***

##背景  

车商发布资源门槛较高，手里的资源想全部转化为平台上的规范资源需要一条一条发布，比较繁琐，但是车商会天天在朋友圈、微信群里发布他们用自然语言“编写”的资源，例如：

>
X1/286000 白，蓝，红，粽⬇14.5点  
GT320/398000 矿白黑 ⬇16.5点  
525/499600 矿白摩卡 ⬇18.5点  
☎18521707911  
全部上海提，店车店票  

需要借助信息抽取，把文本中的核心信息抽取出来，并且形成与底层数据逻辑一致的资源信息

所以，现在的问题转换为典型的自然语言处理问题了

##基本知识  

车商确定一款车是用 ***车型+指导价*** 或者 ***品牌+指导价***的组合信息，例如朗逸1249，帕萨特2229，奔驰3148等，这里1249和124900等价  

所以回头看上面的例子：

X1/286000 白，蓝，红，粽⬇14.5点  

其实就是  品牌：宝马，车型：X1，指导价：286000，4个颜色，售价是下14.5点

##解决方案  

这个项目乍看上去能做，按照传统思路，这就是个序列标注的问题，简单点可以上CRF，复杂点可以上RNN，但是使用监督学习的最大门槛是没有标注数据，即没有ground truth就没办法去train model。到此，监督学习的方案可以打住了，在资源有限的前提下，只能上无监督  

####1. 预处理：  

0. 文本预处理，任何场景都避免不了  

	a. emoji字符  
	
	b. 特殊含义的字符替换，例如“⬇”替换成“下”  
	
	c. 繁体转简体  
	
	d. 全角转半角  
	
	e. 剔除每行的起始标识，例如1. 2、等  
	
	f. 移除类似emoji一样的信息，例如"[微笑]","[鲜花]","[太阳]"等  
	
	g. 把售价标准化，例如"-17w"替换成"下17w"  
	
	h. 剔除日期信息带来的干扰  

1. 分段，切分出一条完整的资源，这里先默认一行是一条有效资源，相同的信息进行去重  

####2. 实体识别与抽取

与业务高度相关，我们无法从文本中直接得到实体，而是通过一些关键内容和关键属性，再基于长期维护的汽车知识库，才能得到真正的"实体"。所以，我们这里定义牛牛实体为"朗逸1249" 或者"320 3259"这种能够辅助我们较为精确的定位到真正的车型实体

所以我们在这一步的目标就是找牛牛实体

1. 分词  
	
	维护汽车领域的专业词库，基于IK，向后匹配最大次，然后切分，有歧义则按照规则消歧  
	
2. "词性标注" -> 属性标注

	上下文无关 依赖词库，但是有歧义，例如"525"，有可能是价格，也有可能是款式，例如"新能源"有可能是车型，也有可能是款式  
	
	上下文有关 基于统计投票的方法，例如"标致 408"，则"408"的属性为 P(Y|标致，408)，那么这里就可以确定408是车型而不是指导价或者款式  

3. 实体拼接、过滤  

	单独的token "标致"或者"408"，甚至是"标致"+"408"，都无法帮助我们在一个较小的范围内确定实体。
	
	所以需要尽量充分的token信息，帮我们直接确定实体，例如"标致408 1497"才可能帮我们确定出比较合理的范围，在这个范围内，即使识别错，也可以: 根据其他信息进行校正；错误可以接受；丢弃  

4. 规格分类  

	不分类也可以解决，但是会导致由于中规国产都是标品，而平行进口均为非标品，二者的解决思路会有一些差异，会为了适应平行进口做太多的折中导致标品的抽取性能也受到影响，所以我们进行文本分类  
	
	浏览过大量的真实车商语料以后，发现并不需要使用复杂的方式；我只需要把品牌、规格、数字信息、配置等显式的特征拿出来，通过单桩决策树就可以了  
	
	为了避免规格分类错误导致后续的灾难，再引入伪相关反馈的方式，增强规格分类的鲁棒性。具体方式为：通过信息抽取得到实体以后，映射到标准的车型知识库中，取top10的候选车型，统计其规格分布，并给出最终规格    
	
5. 实体映射  
	
	至此，我们就已经从"X1/286000 白，蓝，红，粽⬇14.5点"抽取了牛牛实体："国产 X1 286000"，但是我们的目标是真是的汽车实体，所以我们需要把牛牛实体映射到真正的实体

	真正的实体，其实就是车型库里的每一条记录。那么，如何通过"国产 X1 286000"找到车型库中的"国产 宝马 X1 2860"的车型信息呢？

	这里可以看做是文本相似度计算的问题，那么，是用编辑距离？VSM？还是pLSI？LDA？word2vec?  

	但是，在线上使用的时候，总不能对每一个实体，去计算所有车型和它的距离，那样效率太低了，O(k\*k\*N)，N是车型库的量，k是文本的特征长度  

	所以我们要想办法缩小召回的范围，把时间复杂度降低到O(k\*k\*M)。例如，我们通过品牌进行一次筛选，就可以把候选文本的量大规模降低。这里就依赖我们在搜索上的工作，把文本映射成"结构化"的数据：  

		规格：国产 品牌：宝马 车型：X1 款式：sDrive18Li 时尚型  指导价：28.6 年份：2018  

	这样按照不同field来综合计算文本相似度即可。通过倒排表+weight的方式，得到候选结果以及结果的分数，当分数大于某个阈值，该结果才可进入下一步精细化筛选阶段   

	在实体映射过程中，会遇到候选结果过多导致识别困难。此时，需要根据业务场景本身的特性，进行车型、品牌等信息的回溯，解决如下的case：  
	
		朗逸  
		1249 黑白下24000  
		1369 黑黑下25000
	
	因此，要根据LRU的方式更新最近使用的车型、品牌等信息  

6. 颜色抽取  

	得到置信度较高的候选实体后，需要进行颜色的识别。车型知识库中本身带有外观和内饰的标准颜色信息，我们需要把文本中的非标准颜色和标准颜色库一一对应：
	
	a. 颜色token抽取  
		
	b. 特殊颜色分割，生成颜色数组以及颜色的token对应的index的数组  
	
	c. 基于贪心的颜色匹配算法(也可以使用动态规划的方式)，这里使用到的特征较多，多为上下文特征以及颜色本身和标准颜色库的编辑距离等信息   
		
7. 价格解析  

	a. 区分中规国产车和平行进口车  
	
	b. 找到可能的价格token，例如"朗逸1249 2台下2.4w"，这里的"2.4"是价格token，而"2台"对于价格没有实际意义  
	
	c. 判定加价还是降价  
	
	d. 如果是降价，判定下xx点还是下xx万  
	
	e. 如果加价降价不明确，根据行情价来判断是加价还是降价还是直接报价  
	
	f. 如果价格识别置信度较低，则以电议发布  
	
	g. 如果是平行进口车，则在接下来的内容识别中继续搜索可能的价格token来用于支持上一行信息对应的资源定价，基于正则  
	
	**增强价格识别模型：**  
	
	车商发资源，有很多奇葩情况，例如：

	"16款锐界 3198棕 2个 下40000"  
	
	"120-2898曙光金，埃蓝加2900自动泊车29点"  

	各种歧义性的内容导致价格识别错误，一直以来只是用鹰眼系统来"稍微"控制一下资源的准确性  
	
	应对方案如下：  
	
	a. 解析所有文本中可能的报价方案，对每个报价方案进行有效性审核：PriceValidationClassifier。如果该资源的真实价格非常接近于行情价，或者如果该资源没有行情价，但是真实价格距离指导价的价格较为接近，则表明该报价方案是该资源的真实价格，直接返回报价内容即可  

	b. 解析所有的潜在报价方案，且所有的报价都偏差没有非常小，即置信度都不太高，则把报价内容放入数组，直到报价模式全部解析完以后，遍历数组，找一个偏差最小的出来，作为候选  

	c. 得到候选报价方案以后，有两种可能 a)该报价方案正确，正常发布资源 b)该资源已经识别错误了，所以报价方案有偏差，那么就不应该发布该资源。这里我们指定一个值来判定，如果只命中了指导价，而且价格偏差较大，则有很大可能是basecar识别错误，所以不发该资源。否则，发布资源  
	
8. 平行进口车车架号解析  

	基于正则，正则模式存储在文件中，匹配命中以后，把原始文本中的相关车架号内容剔除，因为车架号的纯数字容易给价格的抽取带来干扰    

9. 平行进口车期货现车解析  

	基于正则，正则模式存储在文件中    

10. 备注解析  

	随着信息的抽取，光标会一直后移，所有的流程进行完成以后，剩下的信息可以被粗浅的当做是备注，这也和备注不太重要有关  

####3. 其他模块  

1. 缓存    

	性能优化，使用redis做缓存，key为user_id_内容，value为整个CarResource类信息    

2. 日志  

	扩展性优化，Logger统一使用slf4j  

3. 后处理模块  

	在整体大段文本对应的结果列表中，逻辑上推测不可能出现ABA模式的车型分布，例如三条资源分别为朗逸 尚酷 朗逸，这个时候尚酷的置信度需要重新评估，大概率会剔除  

	后续该逻辑会扩展成为ABCA也重新审核置信度，***TODO***  




