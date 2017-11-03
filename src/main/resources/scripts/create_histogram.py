import matplotlib.pyplot as plt
import operator
from tqdm import tqdm
import numpy as np

def main():
	file_name = "data_histogram.txt"
	str_to_freq = []
	word = []
	with open(file_name, 'r') as f:
		for line in tqdm(f.readlines()):
			name, value = line.split(" ")
			#str_to_freq[name] = int(value.rstrip())
			word.append(name)
			str_to_freq.append(int(value.rstrip()))

	print(word)
	plt.yscale('log')
	plt.plot(np.arange(len(word)), str_to_freq)
	plt.show()

main()