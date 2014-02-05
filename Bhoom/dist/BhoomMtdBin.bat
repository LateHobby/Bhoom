java -Xmx1024M  ^
 -Dttable=true ^
 -DmoveSorter=true ^
 -DnullMoves=true ^
 -Dlmr=false ^
 -DhistoryHeuristic=true ^
 -DkillerMoves=true ^
 -DfutilityPruning=true ^
 -cp bhoom.jar sc.util.UCI MTDFBinaryEngine 

