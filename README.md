# KurosioAuctionSystem
Minecraftサーバー内等でオークションができるプラグイン


## 使い方 / コマンド集
出品したいアイテムを持ち、  
 ``/kac start <開始価格> [入札単位] [半径] ``  

でオークションをスタート。  
指定する半径以内のプレイヤーにオークションの開始とID等が表示されます。  
<デフォルト>  
最低入札単位：1000円  
半径：15ｍ（最大30ｍ）  

``/kac join <ID>``  ー指定IDのオークションに参加  

``/kac bid``　ーオークション参加後、入札単位分 入札  

``/kac bid <金額>``　ー指定金額分入札  

``/kac autobid <金額>``　ー指定金額分まで自動入札  

``/kac leave``　ーオークションから離脱 （最高入札者の場合落札者となります。） 

``/kac exlist``　ー出品中のアイテムの情報を表示 (出品者のみ)  

``/kac cancel``  ーオークションを中止 (出品者のみ)  

##

### License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0).

See the LICENSE file for details.

