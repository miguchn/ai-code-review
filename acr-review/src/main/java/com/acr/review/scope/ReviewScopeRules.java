package com.acr.review.scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 审查范围规则常量（代码内置，项目级追加走 ReviewScopeConfig）。
 * 分类顺序与优先级见 docs/planning/review-scope-policy-m3.2.md 第 4 节。
 */
public final class ReviewScopeRules
{
    /** 高影响规则编码（落决策快照）。优先级即数组顺序：SECURITY > DEPENDENCY > DB_SCRIPT > CONFIG > SIGNATURE > NEW_FILE。 */
    public static final String RULE_SECURITY = "SECURITY";
    public static final String RULE_DEPENDENCY = "DEPENDENCY";
    public static final String RULE_DB_SCRIPT = "DB_SCRIPT";
    public static final String RULE_CONFIG = "CONFIG";
    public static final String RULE_SIGNATURE = "SIGNATURE";
    public static final String RULE_NEW_FILE = "NEW_FILE";

    /** 排除原因编码。 */
    public static final String EXCLUDE_DEFAULT = "DEFAULT_EXCLUDE";
    public static final String EXCLUDE_PROJECT = "PROJECT_EXCLUDE";
    public static final String EXCLUDE_TEST = "TEST_FILE";

    /** 记录类原因编码。 */
    public static final String RECORD_DELETED = "DELETED";
    public static final String RECORD_BINARY = "BINARY";
    public static final String RECORD_RENAME_ONLY = "RENAME_ONLY";
    public static final String RECORD_GITLINK = "GITLINK";
    public static final String RECORD_MODE_ONLY = "MODE_ONLY";
    public static final String RECORD_EMPTY = "EMPTY";

    /** 平台默认排除：锁文件、机器生成/依赖目录、压缩产物。始终生效。 */
    public static final List<String> DEFAULT_EXCLUDE_GLOBS = List.of(
        // 锁文件（有效变更在清单文件，不重复审查机器生成内容）
        "**/package-lock.json", "**/yarn.lock", "**/pnpm-lock.yaml", "**/composer.lock",
        "**/Gemfile.lock", "**/poetry.lock", "**/Pipfile.lock", "**/go.sum", "**/Cargo.lock",
        // 依赖/构建/生成目录
        "**/node_modules/**", "**/vendor/**", "**/dist/**", "**/target/**", "**/build/**",
        "**/generated/**", "**/gen/**",
        // 压缩与映射产物、测试快照
        "**/*.min.js", "**/*.min.css", "**/*.bundle.js", "**/*.map",
        "**/__snapshots__/**", "**/*.snap");

    /** 测试文件（scope_include_tests=N 时排除）。 */
    public static final List<String> TEST_FILE_GLOBS = List.of(
        "**/src/test/**", "**/src/it/**", "**/__tests__/**", "**/testdata/**",
        "**/*Test.java", "**/*Tests.java", "**/*TestCase.java", "**/*_test.go",
        "**/test_*.py", "**/*_test.py",
        "**/*.test.js", "**/*.test.ts", "**/*.test.jsx", "**/*.test.tsx",
        "**/*.spec.js", "**/*.spec.ts", "**/*.spec.jsx", "**/*.spec.tsx");

    /** 权限/安全关键字：命中路径（小写）即按安全逻辑扩展。宁多审不漏审。 */
    public static final List<String> SECURITY_PATH_KEYWORDS = List.of(
        "auth", "login", "logout", "credential", "secret", "crypto", "cipher",
        "encrypt", "decrypt", "password", "permission", "rbac", "token", "jwt", "oauth");

    /** 依赖清单：按文件名精确匹配（不锁版本文件，那些已在默认排除）。 */
    public static final Set<String> DEPENDENCY_MANIFEST_NAMES = Set.of(
        "pom.xml", "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts",
        "package.json", "go.mod", "requirements.txt", "pipfile", "gemfile", "cargo.toml", "composer.json");

    /** 数据库脚本与迁移目录；MyBatis Mapper 含 SQL，归入此类。 */
    public static final List<String> DB_SCRIPT_GLOBS = List.of(
        "**/*.sql", "sql/**", "**/db/migration/**", "**/migrations/**",
        "**/flyway/**", "**/liquibase/**", "**/*Mapper.xml");

    /** 配置文件（依赖清单优先于此类判定；剩余 *.xml 落入此类）。 */
    public static final List<String> CONFIG_FILE_GLOBS = List.of(
        "**/*.yml", "**/*.yaml", "**/*.properties", "**/*.toml", "**/*.ini", "**/*.conf",
        "**/*.xml", "**/.env", "**/.env.*",
        "**/Dockerfile", "**/Dockerfile.*", "**/docker-compose*", "**/Jenkinsfile",
        "**/.github/workflows/**", "**/.gitlab-ci.yml", "**/*.tf");

    /** 公共方法/接口签名正则，按扩展名分派，扫描新增+删除行。 */
    private static final List<Pattern> JAVA_SIGNATURE = List.of(
        Pattern.compile("^\\s*(public|protected)\\s+[\\w<>\\[\\],.? ]+\\s+\\w+\\s*\\("),
        Pattern.compile("^\\s*(public\\s+)?(final\\s+|abstract\\s+)?(class|interface|enum|record)\\s+\\w+"));
    private static final List<Pattern> JS_SIGNATURE = List.of(
        Pattern.compile("^\\s*export\\s+(default\\s+)?(async\\s+)?(function\\*?\\s+\\w+|class\\s+\\w+|const\\s+\\w+|let\\s+\\w+|interface\\s+\\w+|type\\s+\\w+|enum\\s+\\w+)"),
        Pattern.compile("^\\s*(public|protected)\\s+(static\\s+)?(async\\s+)?\\w+\\s*\\("));
    private static final List<Pattern> PYTHON_SIGNATURE = List.of(
        Pattern.compile("^\\s*(async\\s+)?def\\s+\\w+\\s*\\("),
        Pattern.compile("^\\s*class\\s+\\w+"));
    private static final List<Pattern> GO_SIGNATURE = List.of(
        Pattern.compile("^func\\s+(\\([^)]*\\)\\s+)?[A-Z]\\w*\\s*\\("),
        Pattern.compile("^type\\s+[A-Z]\\w+\\s+(struct|interface)\\b"));

    private static final Map<String, List<Pattern>> SIGNATURE_BY_EXTENSION = Map.ofEntries(
        Map.entry("java", JAVA_SIGNATURE),
        Map.entry("js", JS_SIGNATURE),
        Map.entry("jsx", JS_SIGNATURE),
        Map.entry("ts", JS_SIGNATURE),
        Map.entry("tsx", JS_SIGNATURE),
        Map.entry("vue", JS_SIGNATURE),
        Map.entry("py", PYTHON_SIGNATURE),
        Map.entry("go", GO_SIGNATURE));

    private ReviewScopeRules()
    {
    }

    public static boolean matchesAny(List<String> globs, String path)
    {
        for (String glob : globs)
        {
            if (GlobPattern.matches(glob, path))
            {
                return true;
            }
        }
        return false;
    }

    /** 路径是否命中安全关键字（小写包含匹配）。 */
    public static boolean isSecurityPath(String path)
    {
        if (path == null)
        {
            return false;
        }
        String lower = path.toLowerCase();
        for (String keyword : SECURITY_PATH_KEYWORDS)
        {
            if (lower.contains(keyword))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isDependencyManifest(String path)
    {
        if (path == null)
        {
            return false;
        }
        int slash = path.lastIndexOf('/');
        String name = (slash >= 0 ? path.substring(slash + 1) : path).toLowerCase();
        return DEPENDENCY_MANIFEST_NAMES.contains(name);
    }

    /** 变更行是否命中公共签名（按扩展名选择规则集；不支持的扩展名返回 false）。 */
    public static boolean hitsPublicSignature(String path, List<String> changedLines)
    {
        if (path == null || changedLines == null || changedLines.isEmpty())
        {
            return false;
        }
        int dot = path.lastIndexOf('.');
        if (dot < 0)
        {
            return false;
        }
        List<Pattern> patterns = SIGNATURE_BY_EXTENSION.get(path.substring(dot + 1).toLowerCase());
        if (patterns == null)
        {
            return false;
        }
        for (String line : changedLines)
        {
            for (Pattern pattern : patterns)
            {
                if (pattern.matcher(line).find())
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * OCR 路径 --exclude 用的完整排除规则集（M3.2 步 6）：
     * 平台默认排除 + 测试文件（scope_include_tests=N 时）+ 项目追加，去重保序。
     * 与决策服务的排除判定保持同一数据源，保证两条路径口径一致。
     */
    public static List<String> mergedExcludeGlobs(ReviewScopeConfig config)
    {
        ReviewScopeConfig effective = config == null ? ReviewScopeConfig.defaults() : config;
        List<String> merged = new ArrayList<>(DEFAULT_EXCLUDE_GLOBS);
        if (!effective.includeTests())
        {
            merged.addAll(TEST_FILE_GLOBS);
        }
        for (String pattern : effective.excludePatterns())
        {
            if (!merged.contains(pattern))
            {
                merged.add(pattern);
            }
        }
        return List.copyOf(merged);
    }
}
