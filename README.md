# ShadeRegen
資源ワールド向け、ワールド再生成プラグイン

## 特徴
- 別プロセスでワールド生成するので、サーバーティックを全く止めない
- 事前に生成しておくことで一瞬でワールドを切り替えできる

### できないこと
- ワールドのシード値やゲームルールの保持
- プラグインによる生成処理の介入
- 比較的生成に時間がかかる

### TODO
- ワールド内プレイヤーの退避
- 自動実行スケジュール
- コマンド (現在、最小限の機能のみ)
- Chunky との組み合わせに対応する

## 前提
- Spigot 1.13 以上
- [Multiverse-Core](https://github.com/multiverse/multiverse-core) (v2~5 まで対応)
- [KyoriAdventureLib](https://github.com/Necnion8/KyoriAdventureLib) (Paper 1.16.5 以降なら不要)
