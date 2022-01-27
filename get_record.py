# paste the run from the queue tab into results.txt
# start from the actual first match not the header "MPTempName vs Whoever"
# run this script and look at results.csv
# go into spreadsheet and click the cell you want to insert records at
# go to file -> import -> upload -> results.csv -> replace data at current cell
# bam the records are there

import csv

file = open('results.txt', 'r')
lines = file.readlines()
total = 0
wins = 0
records = []
for line in lines:
    res = ""
    total += 1
    splitted = line.split()
    if splitted[2] != "MPTempName":
        res += "L "
    else:
        res += "W "
        wins += 1
    res += splitted[5]
    records.append(res)
csvFile = "records.csv"
with open(csvFile, 'w') as f:
    csvwriter = csv.writer(f) 
    csvwriter.writerow(records)
print(wins / total)