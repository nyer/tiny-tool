import groovy.sql.Sql
import java.sql.ResultSetMetaData

/**
    数据表导入工具，能将线上的数据导入到测试环境
    支持级联，即能导入以表A中的字段为外键的表
    
    @author ls5231#gmail.com
**/



class DBQueryer {
    private dao
    DBQueryer(url, username, password, driver) {
       dao = Sql.newInstance(url, username, password, driver)
    }
    DBQueryer(url, driver) {
       dao = Sql.newInstance(url, driver)
    }
    
    def execute(sql, param, func) {
        dao.eachRow(sql, param, func)
    }
    
    def executeUpdate(sql, param) {
        dao.executeUpdate(sql, param)
    }
    def run(sql, param) {
        dao.execute(sql, param)
    }
}

driver = "com.mysql.jdbc.Driver"
//dest db url
testurl = "jdbc:mysql://127.0.0.1:3306/hello"
//source db url
onlineurl = "jdbc:mysql://127.0.0.1:3307/hello"


onlineQueryer = new DBQueryer(onlineurl, driver)
testQueryer = new DBQueryer(testurl, driver)

//将线上用户的数据导入到测试环境里某个用户下面
onlineAccount = 'yuxiao1a@163.com';
testAccount = 'justaid@163.com';

onlineUserId = null;
testUserId = null;

//拿到各自对应的UserId
onlineQueryer.execute('select UserId from Account where UserName = ?', [onlineAccount], {t -> onlineUserId = t.UserId});
testQueryer.execute('select UserId from Account where UserName = ?', [testAccount], {t -> testUserId = t.UserId});

println '线上用户id: ' + onlineUserId
println '测试用户id: ' + testUserId

class Table {
    private onlineQueryer;
    private onlineCascadeKey;
    private testQueryer;
    private testCascadeKey;
    private table;
    private cleanSQLFunc;
    private selectSQLFunc;
    private insertFunc;
   
    //默认的insert_func
    private static DEFAULT_INSERT_FUNC = { onlineRowMap, testCasecadeRowMap -> return onlineRowMap;}
    
    //
    private onlineCascadeRowMap;
    private testCascadeRowMap;
    
    private batchSize = 512;//批量导入
    
    //级联导入的表
    private cascadeSons = [];
    
    Table(onlineQueryer, onlineCascadeRowMap, testQueryer,testCascadeRowMap, table,
     cleanSQLFunc, selectSQLFunc, insertFunc) {
        this.onlineQueryer = onlineQueryer;
        this.onlineCascadeRowMap = onlineCascadeRowMap;
        this.testQueryer = testQueryer;
        this.testCascadeRowMap= testCascadeRowMap;
        this.table = table;
        this.cleanSQLFunc = cleanSQLFunc;
        this.selectSQLFunc = selectSQLFunc;
        this.insertFunc = insertFunc;
    }
    
    //将ResultSet转为Map
    private def rowToMap(row) {
        def key_value = [:];
        def meta = row.getMetaData();
        def columnCount = meta.getColumnCount();
        for (i in 1..columnCount) {
            def cn = meta.getColumnName(i);
            def value = row.getObject(i);
            key_value.put(cn, value);
        }
        
        return key_value;
    }
    
    //清理测试环境的数据
    def cleanTest() {
        println "开始清理 " + table;
        def sql_params = cleanSQLFunc(table, testCascadeRowMap);
        println '清理sql: ' + sql_params[0] + ',参数: ' + sql_params[1];
        this.testQueryer.run(sql_params[0], sql_params[1]);
        println "清理成功 " + table;
    }
    
    //将线上环境的数据导入到测试环境
    def flush() {
        def start = System.nanoTime();;
        println "开始导入 " + table;
        
        //拿到线上环境的查询语句以及参数, 这里的语句和参数不包括limit部分
        def sql_params = selectSQLFunc(table, onlineCascadeRowMap);
        println '查询sql: ' + sql_params[0] + ',参数: ' + sql_params[1];
        
        def rowMaps = [];//保存当前导入的全部数据，用于后面的级联表导入
        
        def sql_tpl = null;
        def param_tpl = null;
        
        def keys = [];//包括表的所有Column name
        def key_value = [:];//一行RowSet的column_name - value形式
        
        def offset = - batchSize;//用于批量查询
        def hasNext = true;
        
        sql_params[1].add(offset);
        sql_params[1].add(batchSize);
        
        while (hasNext) {
            hasNext = false;
            offset += batchSize;
            sql_params[1].set(1, offset);
            
            def values = [];
            def insert_sql = null;
            
            def limitedSql = sql_params[0] + " limit ?, ?";
            onlineQueryer.execute(limitedSql , sql_params[1], { row ->
                rowMaps.add(rowToMap(row));
                
                //初始化sql模板
                if (sql_tpl == null) {
                    def meta = row.getMetaData();
                    def columnCount = meta.getColumnCount();
                    for (i in 1..columnCount) {
                        def cn = meta.getColumnName(i);
                        keys.add(cn);
                        key_value.put(cn, null);
                    }
                    sql_tpl = new StringBuilder("insert into ").append(table).append("(");
                    keys.each{
                        sql_tpl.append(it).append(",");
                    };
                    sql_tpl.deleteCharAt(sql_tpl.length() -1);
                    sql_tpl.append(")").append(" values ");
                    
                    param_tpl = new StringBuilder("( ");
                    param_tpl = param_tpl.append("?," * (keys.size()));
                    param_tpl.deleteCharAt(param_tpl.length() -1);
                    param_tpl.append(" )");
                }
                
                if (insert_sql == null ) {
                    insert_sql = new StringBuilder(sql_tpl);
                }
                insert_sql.append(param_tpl).append(",");
                
                key_value.clear();
                for (i in 1..keys.size()) {
                    def value = row.getObject(i);
                    key_value.put(keys.get(i-1), value);
                }
                key_value = insertFunc(key_value, testCascadeRowMap);
                keys.each{
                    values.add(key_value.get(it));
                };
                //有可能有下一批
                hasNext = true;
            });
            if (insert_sql != null) {
                insert_sql.deleteCharAt(insert_sql.length() -1);
                testQueryer.run(insert_sql.toString(), values);
            }
        }
        
        def end = System.nanoTime();
        
        println "导入成功..., 耗时: " + ((end - start) /1000000) + "毫秒\n\n";
        
        //级联更新
        this.cascadeSons.each{
            def t = it;
            println "级联更新: " + it.table
            rowMaps.each{
                def r = it;
                t.testCascadeRowMap = t.onlineCascadeRowMap = r;
                t.update();
            };
        };
    }
    
    /**
    ***综合了清理数据与导数据
    **/
    def update() {
        this.cleanTest();
        this.flush();
    }
    
    /**
    *** 添加级联导入的表
    **/
    def addCascadeTable(table, cleanSQLFunc, selectSQLFunc) {
        Table t = new Table(onlineQueryer, null, testQueryer, null, table, cleanSQLFunc, selectSQLFunc, DEFAULT_INSERT_FUNC);
        this.cascadeSons.add(t);
        return t;
    }
    
    def String toString() {
        return table + "";
    }
}
//要导的表
statBookSell = new Table(onlineQueryer, ["UserId" : onlineUserId], testQueryer, ["UserId" : testUserId],
    'StatBookSell',
    {table, rowMap -> return ["delete from " + table + " where UserId = ? and PartnerType not in (0,2)", [rowMap.UserId]]},
    {table, rowMap -> return ["select * from " + table + " where UserId = ? and PartnerType not in (0,2)" , [rowMap.UserId]]},
    {onlineRowMap, testCasecadeRowMap -> 
        onlineRowMap.put("UserId", testCasecadeRowMap.UserId);
        return onlineRowMap;
    }
);

bookSource = new Table(onlineQueryer, ["UserId" : onlineUserId], testQueryer, ["UserId" : testUserId],
    'BookSource',
    {table, rowMap -> return ["delete from " + table + " where Partner = ? and PartnerType not in (0,2)", [rowMap.UserId]]},
    {table, rowMap -> return ["select * from " + table + " where Partner = ? and PartnerType not in (0,2)" , [rowMap.UserId]]},
    {onlineRowMap, testCasecadeRowMap -> 
        onlineRowMap.put("Partner", testCasecadeRowMap.UserId);
        return onlineRowMap;
    }
);

//添加级联更新的表
bookSource.addCascadeTable("BookArticle", 
    {table, rowMap -> return ["delete from " + table + " where SourceUuid = ? " , [rowMap.SourceUuid]]},
    {table, rowMap -> return ["select * from " + table + " where SourceUuid = ?" , [rowMap.SourceUuid]]}
); 

tables = [statBookSell, bookSource];

//要导的表
println "要导的表:" + tables;

//开始
println "开始导入...\n\n"
start = System.nanoTime();
for (table in tables) {
    table.cleanTest();
    table.flush();
}
end = System.nanoTime();
println "导入成功..., 总耗时: " + (end - start) /1000000 + "毫秒\n\n";
