package com.mimo.analysis.core.model;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分析报告模型，汇总所有类信息和引用关系的完整分析结果。
 *
 * 这是整个分析流程的核心数据结构，负责：
 * - 存储所有被发现的类信息（{@link ClassInfo}）
 * - 存储所有类之间的引用关系（{@link ClassReference}）
 * - 按类名去重类信息，按（来源类, 目标类, 引用类型）三元组去重引用关系
 * - 维护按包名分组的统计信息（{@link PackageStats}）
 * - 支持合并多个报告（Agent 和静态分析的结果合并）
 *
 * 使用 {@link LinkedHashMap} 保持类的插入顺序，使用 {@link HashSet} 进行引用去重。
 */
public class AnalysisReport {

    /** 所有类信息，按全限定名索引，保持插入顺序 */
    private final Map<String, ClassInfo> classes;

    /** 所有引用关系列表，保持插入顺序 */
    private final List<ClassReference> references;

    /** 引用关系的唯一标识键集合，用于快速去重判断 */
    private final Set<String> uniqueReferenceKeys;

    /** 按包名分组的统计信息 */
    private final Map<String, PackageStats> packageStats;

    /** 报告生成时间戳 */
    private final Instant generatedAt;

    /** 分析模式标识，如 "static"、"agent"、"combine" */
    private final String analysisMode;

    /**
     * 构造指定分析模式的空报告实例。
     *
     * @param analysisMode 分析模式标识
     */
    public AnalysisReport(String analysisMode) {
        this.classes = new LinkedHashMap<>();
        this.references = new ArrayList<>();
        this.uniqueReferenceKeys = new HashSet<>();
        this.packageStats = new LinkedHashMap<>();
        this.generatedAt = Instant.now();
        this.analysisMode = analysisMode;
    }

    /**
     * 添加一个类信息到报告中。
     * 如果该类已存在（按全限定名判断），则忽略重复添加。
     * 添加成功时会同步更新包统计信息。
     *
     * @param info 要添加的类信息
     */
    public void addClass(ClassInfo info) {
        String name = info.getClassName();
        if (!classes.containsKey(name)) {
            classes.put(name, info);
            addClassToPackageStats(info);
        }
    }

    /**
     * 批量添加类信息到报告中。
     *
     * @param infos 要添加的类信息集合
     */
    public void addClasses(Collection<ClassInfo> infos) {
        for (ClassInfo info : infos) {
            addClass(info);
        }
    }

    /**
     * 添加一条引用关系到报告中。
     * 使用 {@link ClassReference#toKey()} 生成的唯一键进行去重，
     * 相同（来源类, 目标类, 引用类型）三元组的引用只保留一条。
     *
     * @param ref 要添加的引用关系
     */
    public void addReference(ClassReference ref) {
        String key = ref.toKey();
        if (uniqueReferenceKeys.add(key)) {
            references.add(ref);
        }
    }

    /**
     * 批量添加引用关系到报告中。
     *
     * @param refs 要添加的引用关系集合
     */
    public void addReferences(Collection<ClassReference> refs) {
        for (ClassReference ref : refs) {
            addReference(ref);
        }
    }

    /**
     * 合并另一个分析报告到当前报告中。
     *
     * 合并逻辑：
     * - 类信息：按类名去重，如果同一个类在两个报告中都存在，
     *   且来源不同（如一个来自 Agent，一个来自静态分析），
     *   则将来源更新为 {@link ClassOrigin#BOTH}。
     * - 引用关系：按三元组去重后追加。
     *
     * @param other 要合并的另一个报告
     */
    public void merge(AnalysisReport other) {
        // 合并类信息，处理来源冲突
        for (Map.Entry<String, ClassInfo> entry : other.classes.entrySet()) {
            String name = entry.getKey();
            ClassInfo otherInfo = entry.getValue();

            if (this.classes.containsKey(name)) {
                // 类已存在，检查来源是否不同，如果不同则标记为 BOTH
                ClassInfo existing = this.classes.get(name);
                if (existing.getOrigin() != otherInfo.getOrigin()) {
                    this.classes.put(name, existing.toBuilder()
                            .origin(ClassOrigin.BOTH)
                            .build());
                }
            } else {
                // 新类，直接添加
                this.classes.put(name, otherInfo);
                addClassToPackageStats(otherInfo);
            }
        }

        // 合并引用关系（自动去重）
        for (ClassReference ref : other.references) {
            addReference(ref);
        }
    }

    /**
     * 获取所有类信息的不可修改集合。
     *
     * @return 类信息集合
     */
    public Collection<ClassInfo> getAllClasses() {
        return Collections.unmodifiableCollection(classes.values());
    }

    /**
     * 获取所有引用关系的不可修改列表。
     *
     * @return 引用关系列表
     */
    public List<ClassReference> getAllReferences() {
        return Collections.unmodifiableList(references);
    }

    /**
     * 获取按包名分组的统计信息的不可修改映射。
     *
     * @return 包名到统计信息的映射
     */
    public Map<String, PackageStats> getPackageStats() {
        return Collections.unmodifiableMap(packageStats);
    }

    /**
     * 获取指定包下的所有类信息。
     *
     * @param packageName 包名
     * @return 该包下的类信息列表
     */
    public List<ClassInfo> getClassesByPackage(String packageName) {
        return classes.values().stream()
                .filter(c -> c.getPackageName().equals(packageName))
                .collect(Collectors.toList());
    }

    /**
     * 获取从指定类出发的所有引用关系（该类作为引用来源）。
     *
     * @param className 类的全限定名
     * @return 从该类出发的引用关系列表
     */
    public List<ClassReference> getReferencesFrom(String className) {
        return references.stream()
                .filter(r -> r.getFromClass().equals(className))
                .collect(Collectors.toList());
    }

    /**
     * 获取指向指定类的所有引用关系（该类作为被引用目标）。
     *
     * @param className 类的全限定名
     * @return 指向该类的引用关系列表
     */
    public List<ClassReference> getReferencesTo(String className) {
        return references.stream()
                .filter(r -> r.getToClass().equals(className))
                .collect(Collectors.toList());
    }

    /**
     * 根据全限定名获取单个类信息。
     *
     * @param className 类的全限定名
     * @return 类信息，如果不存在则返回 null
     */
    public ClassInfo getClass(String className) {
        return classes.get(className);
    }

    /**
     * 获取报告中的类总数。
     *
     * @return 类总数
     */
    public int getTotalClassCount() {
        return classes.size();
    }

    /**
     * 获取报告中的引用关系总数。
     *
     * @return 引用关系总数
     */
    public int getTotalReferenceCount() {
        return references.size();
    }

    /**
     * 获取被引用次数最多的 Top N 类。
     * 统计每个类作为引用目标出现的次数，按降序排列。
     *
     * @param limit 返回的最大数量
     * @return 类名与被引用次数的键值对列表
     */
    public List<Map.Entry<String, Integer>> getTopReferencedClasses(int limit) {
        Map<String, Integer> countMap = new HashMap<>();
        for (ClassReference ref : references) {
            countMap.merge(ref.getToClass(), 1, Integer::sum);
        }
        return countMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取类数量最多的 Top N 包。
     * 按包下类数量降序排列。
     *
     * @param limit 返回的最大数量
     * @return 包名与类数量的键值对列表
     */
    public List<Map.Entry<String, Integer>> getTopPackages(int limit) {
        return packageStats.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getClassCount()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** 获取报告生成时间戳 */
    public Instant getGeneratedAt() { return generatedAt; }

    /** 获取分析模式标识 */
    public String getAnalysisMode() { return analysisMode; }

    /**
     * 将类信息添加到对应的包统计中。
     * 如果该包的统计信息尚未创建，会自动创建一个新的 PackageStats 实例。
     *
     * @param info 类信息
     */
    private void addClassToPackageStats(ClassInfo info) {
        String pkg = info.getPackageName();
        PackageStats stats = packageStats.computeIfAbsent(pkg, PackageStats::new);
        stats.addClass(info);
    }
}
