package com.example.springaidemo.model.qualityscheme;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预定义检查项清单（共 27 项）。
 * <p>
 * 使用 record + ParamSpec 结构定义，替代原 QualitySchemeService 中的 RAW_CHECK_ITEMS（String[][]）。
 * 每个检查项的 dataName（图层名称）在外层，不在 params 中。
 * 其他参数名保持原始命名（snake_case 和 camelCase 混用，与后端 API 一致）。
 * <p>
 * 涵盖质检能力域：字段检查、几何检查、坐标系检查、图层一致性、值域、时间有效性、编码匹配等。
 *
 * @author spring-ai-demo
 */
public final class CheckItemDefinitions {

    private CheckItemDefinitions() {}

    /**
     * 27 项预定义检查项定义列表。
     */
    public static final List<CheckItemDefinition> DEFINITIONS = List.of(
            // 1. 字段长度检查
            new CheckItemDefinition(
                    "qualityCheckFieldLength",
                    "字段长度检查",
                    "检查字段长度是否符合定义规范",
                    List.of(
                            new ParamSpec("fieldNames", "要检查的字段名称，多个字段用英文逗号隔开", "id,name"),
                            new ParamSpec("fieldLengths", "长度要求，如10表示字段长度需小于10", "10")
                    )
            ),
            // 2. 必填值非空且不完全相同
            new CheckItemDefinition(
                    "qualityCheckRequiredFieldMismatch",
                    "必填值非空且不完全相同",
                    "检查必填字段非空且值不完全一致",
                    List.of(
                            new ParamSpec("fieldNames", "要检查的字段名称，多个字段用英文逗号隔开", "id,name")
                    )
            ),
            // 3. 字段唯一值检查
            new CheckItemDefinition(
                    "QualityCheckUniqueValue",
                    "字段唯一值检查",
                    "检查字段值是否唯一不重复",
                    List.of(
                            new ParamSpec("fieldNames", "需检查唯一性的字段名，多个字段用英文逗号隔开", "id")
                    )
            ),
            // 4. 时间有效性检查
            new CheckItemDefinition(
                    "QualityCheckTimeValidity",
                    "时间有效性检查",
                    "检查时间字段是否在有效时间范围内",
                    List.of(
                            new ParamSpec("fieldNames", "时间字段名，多个字段用英文逗号隔开", "create_time"),
                            new ParamSpec("dateStart", "有效起始日期", "2020-01-01"),
                            new ParamSpec("dateEnd", "有效结束日期", "2025-12-31")
                    )
            ),
            // 5. 线重叠检查（无额外参数）
            new CheckItemDefinition(
                    "qualityCheckLineOverlap",
                    "线重叠检查",
                    "检查线要素是否存在重叠重合",
                    List.of()
            ),
            // 6. 点重叠检查（无额外参数）
            new CheckItemDefinition(
                    "QualityCheckPointOverlap",
                    "点重叠检查",
                    "检查点要素是否存在重叠重合",
                    List.of()
            ),
            // 7. 线悬挂点检查（无额外参数）
            new CheckItemDefinition(
                    "QualityInspectionLayerHangingPoints",
                    "线悬挂点检查",
                    "检查线要素是否存在悬挂点",
                    List.of()
            ),
            // 8. 碎线检查
            new CheckItemDefinition(
                    "QualityCheckInnerLayerBreaks",
                    "碎线检查",
                    "检查线要素是否存在小于最小长度的碎线",
                    List.of(
                            new ParamSpec("min_length", "最小线长度阈值（米），小于此值为碎线", "0.5")
                    )
            ),
            // 9. 面内重叠检查（无额外参数）
            new CheckItemDefinition(
                    "qualityInspectionFeatureOverlap",
                    "面内重叠检查",
                    "检查面要素内部是否存在重叠",
                    List.of()
            ),
            // 10. 面缝隙检查（无额外参数）
            new CheckItemDefinition(
                    "QualityInspectionSurfaceGapCheck",
                    "面缝隙检查",
                    "检查相邻面之间是否存在缝隙",
                    List.of()
            ),
            // 11. 碎面检查
            new CheckItemDefinition(
                    "QualityCheckInnerLayerFragments",
                    "碎面检查",
                    "检查面要素是否存在小于最小面积的碎面",
                    List.of(
                            new ParamSpec("min_area", "最小面面积阈值（平方米），小于此值为碎面", "10")
                    )
            ),
            // 12. 尖锐角检查
            new CheckItemDefinition(
                    "SharpAngleCheckForQC",
                    "尖锐角检查",
                    "检查要素是否存在小于阈值的尖锐角",
                    List.of(
                            new ParamSpec("min_angle", "最小角度阈值（度），小于此值为尖锐角", "30")
                    )
            ),
            // 13. 面自相交检查（无额外参数）
            new CheckItemDefinition(
                    "QualityCheckSurfaceSelfIntersection",
                    "面自相交检查",
                    "检查面要素是否存在自相交问题",
                    List.of()
            ),
            // 14. 面要素空洞检查（无额外参数）
            new CheckItemDefinition(
                    "QualityCheckVoidInspection",
                    "面要素空洞检查",
                    "检查面要素是否存在无效空洞",
                    List.of()
            ),
            // 15. 面积与记录值一致性
            new CheckItemDefinition(
                    "layerPolygonAreaConsistencyCheck",
                    "面积与记录值一致性",
                    "检查图层面要素面积与记录值是否一致",
                    List.of(
                            new ParamSpec("fieldNames", "面积字段名，多个字段用英文逗号隔开", "area"),
                            new ParamSpec("threshold", "面积误差阈值，如0.1表示误差不超过10%", "0.1"),
                            new ParamSpec("unit", "面积单位", "平方米")
                    )
            ),
            // 16. 小数位数检查
            new CheckItemDefinition(
                    "qualityCheckDecimalPlaces",
                    "小数位数检查",
                    "检查数值字段小数位数是否符合要求",
                    List.of(
                            new ParamSpec("fieldNames", "数值字段名，多个字段用英文逗号隔开", "price"),
                            new ParamSpec("fieldScales", "小数位数要求，如2表示小数不超过2位", "2")
                    )
            ),
            // 17. 范围值域检查
            new CheckItemDefinition(
                    "QualityCheckRangeValidation",
                    "范围值域检查",
                    "检查字段值是否在规定值域范围内，主要用于枚举值，如性别：1,2",
                    List.of(
                            new ParamSpec("fieldNames", "字段名，多个字段用英文逗号隔开", "type"),
                            new ParamSpec("fieldValues", "允许的值域列表，多个用英文逗号隔开", "1,2,3")
                    )
            ),
            // 18. 字段非法字符检查
            new CheckItemDefinition(
                    "qualityCheckInvalidFieldValue",
                    "字段非法字符检查",
                    "检查字段值是否包含非法字符",
                    List.of(
                            new ParamSpec("fieldNames", "字段名，多个字段用英文逗号隔开", "name")
                    )
            ),
            // 19. 编码名称匹配检查
            new CheckItemDefinition(
                    "qualityCheckCodeNameMatch",
                    "编码名称匹配检查",
                    "检查字段编码与名称是否匹配一致",
                    List.of(
                            new ParamSpec("fieldNames", "编码字段名，多个字段用英文逗号隔开", "code"),
                            new ParamSpec("fieldValues", "对应的名称值列表，多个用英文逗号隔开", "居住用地,商业用地")
                    )
            ),
            // 20. 平面坐标系检查（无额外参数）
            new CheckItemDefinition(
                    "QualityCheckCoordinateSystem",
                    "平面坐标系检查",
                    "检查图层是否使用平面坐标系",
                    List.of()
            ),
            // 21. 字段必填非空
            new CheckItemDefinition(
                    "qualityCheckFieldRequiredValidation",
                    "字段必填非空",
                    "检查指定字段是否必填且不为空",
                    List.of(
                            new ParamSpec("fieldNames", "必填字段名，多个字段用英文逗号隔开", "id,name")
                    )
            ),
            // 22. 属性字段完整性
            new CheckItemDefinition(
                    "QualityCheckFieldIntegrity",
                    "属性字段完整性",
                    "检查属性字段是否完整、符合规范",
                    List.of(
                            new ParamSpec("fieldNames", "字段名，多个字段用英文逗号隔开", "id,name"),
                            new ParamSpec("fieldTypes", "字段类型要求，多个用英文逗号隔开", "String,Integer"),
                            new ParamSpec("fieldLengths", "字段长度要求，多个用英文逗号隔开", "10,20")
                    )
            ),
            // 23. 空几何检查（无额外参数）
            new CheckItemDefinition(
                    "checkLayerElementEmptyGeometry",
                    "空几何检查",
                    "检查图层要素是否存在空几何对象",
                    List.of()
            ),
            // 24. 图层完整性
            new CheckItemDefinition(
                    "QualityCheckLayerIntegrity",
                    "图层完整性",
                    "检查图层数据完整性",
                    List.of(
                            new ParamSpec("geometry_type", "要求的几何类型，如Point、LineString、Polygon", "Polygon")
                    )
            ),
            // 25. 图层间属性一致性
            new CheckItemDefinition(
                    "checkInterLayerAttributeConsistency",
                    "图层间属性一致性",
                    "检查不同图层之间属性信息是否一致",
                    List.of(
                            new ParamSpec("dz_data_name", "对照图层名称", "xzq"),
                            new ParamSpec("compare_fields_first", "主图层比较字段名", "code"),
                            new ParamSpec("compare_fields_second", "对照图层比较字段名", "dm")
                    )
            ),
            // 26. 多边形被包含且属性相等
            new CheckItemDefinition(
                    "CheckPolygonContainedAttrEqual",
                    "多边形被包含且属性相等",
                    "检查多边形是否被包含且对应属性值相等",
                    List.of(
                            new ParamSpec("dz_data_name", "对照图层名称", "xzq"),
                            new ParamSpec("compare_fields_first", "主图层比较字段名", "code"),
                            new ParamSpec("compare_fields_second", "对照图层比较字段名", "dm")
                    )
            ),
            // 27. 图层间空间属性一致性
            new CheckItemDefinition(
                    "InterLayerFeatureConsistencyCheck",
                    "图层间空间属性一致性",
                    "检查不同图层之间空间与属性信息是否一致",
                    List.of(
                            new ParamSpec("dz_data_name", "对照图层名称", "xzq"),
                            new ParamSpec("condition", "关联条件表达式，如code=dm", "code=dm"),
                            new ParamSpec("key_field_first", "主图层关联键字段", "id"),
                            new ParamSpec("key_field_second", "对照图层关联键字段", "fid"),
                            new ParamSpec("compare_fields_first", "主图层比较字段名", "name"),
                            new ParamSpec("compare_fields_second", "对照图层比较字段名", "dzm")
                    )
            )
    );

    /**
     * checkCode → CheckItemDefinition 映射，用于 O(1) 查询。
     */
    private static final Map<String, CheckItemDefinition> DEFINITION_BY_CODE =
            DEFINITIONS.stream().collect(Collectors.toMap(CheckItemDefinition::checkCode, d -> d));

    /**
     * 按 checkCode 查询检查项定义。
     *
     * @param checkCode 检查项编码
     * @return 对应的定义，不存在返回 null
     */
    public static CheckItemDefinition get(String checkCode) {
        return DEFINITION_BY_CODE.get(checkCode);
    }

    /**
     * 判断 checkCode 是否存在于预定义清单。
     *
     * @param checkCode 检查项编码
     * @return 存在返回 true
     */
    public static boolean isValid(String checkCode) {
        return DEFINITION_BY_CODE.containsKey(checkCode);
    }
}
