<img width="803" height="595" alt="image" src="https://github.com/user-attachments/assets/599b1af8-d8bc-4eca-8100-55006af5748f" />



！表1 加入asset_type VARCHAR 统计资产类型

表1：Company Info (公司基础信息表)
作为参考数据表，存储股票代码与公司名称的映射。
字段名 (Field),数据类型 (Type),约束 (Key),字段说明 (Description)
ticker_symbol,String,PK,股票代码 (例如：AAPL)
company_name,String,,公司全称
asset_type String， 资产类型

表2：Market Data (市场历史数据表)
记录历史股票价格，用于绘制走势图和计算历史表现。

字段名 (Field),数据类型 (Type),约束 (Key),字段说明 (Description)
ticker_symbol,String,"PK, FK",关联表1的股票代码
timestamp,Timestamp,PK,记录的具体时间
close,Decimal,,历史收盘价

表3：Portfolio (投资组合主表)
核心业务表，记录投资组合的顶层信息与当前现金状态。

字段名 (Field),数据类型 (Type),约束 (Key),字段说明 (Description)
portfolio_id,String / UUID,PK,投资组合唯一识别码
name,String,,投资组合名称
cash_balance,Decimal,,可用现金余额

表4：Position (当前持仓表)
记录某投资组合在当下的持仓快照（由交易流水计算得出）。
字段名 (Field),数据类型 (Type),约束 (Key),字段说明 (Description)
position_id,String / UUID,PK,持仓记录唯一识别码
portfolio_id,String / UUID,FK,关联的投资组合 (表3)
ticker_symbol,String,FK,关联的股票代码 (表1)
total_quantity,Decimal / Int,,当前总持有数量 (买入总量减去卖出总量)
average_cost,Decimal,,平均持仓成本

表5：Transaction (交易流水表)
不可篡改的单点真实数据源 (Single Source of Truth)，记录所有的资金与资产动作。
字段名 (Field),数据类型 (Type),约束 (Key),字段说明 (Description)
transaction_id,String / UUID,PK,交易流水唯一识别码
portfolio_id,String / UUID,FK,关联的投资组合 (表3)
ticker_symbol,String,FK,关联的股票代码 (存取款等纯现金流时可为 Null)
transaction_type,Enum / String,,"交易类型：BUY (买入), SELL (卖出), DEPOSIT (存现金), WITHDRAW (提现)"
quantity,Decimal / Int,,交易数量 (靠交易类型区分进出)
price_per_unit,Decimal,,成交单价
transaction_date,Timestamp,,交易发生的精确时间


## HOW to RUN This PROJECT:

pull Branch DockerImp;
new a file : .env
in .env file, input : OPENAI_API_KEY=sk-xxxxx(keyhere)


install docker :   https://www.docker.com/products/docker-desktop/   
If MacOS, choose MAC-APPLE  Silicon

After Installing,   in Terminal, cd to this project, INPUT:
docker-compose up --build -d
OR
docker-compose up --build     

http://localhost:8090 is now running the App


To shut this app down:
docker-compose down 
in TERMINAL

