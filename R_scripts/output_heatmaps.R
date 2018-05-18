# Requires PC to have ImageMagick

library(tidyverse)
library(RColorBrewer)
library(gtools)

# create dir if needed
plotsDir = "plots"
dir.create(file.path(plotsDir), showWarnings = FALSE)

# read in timepoints in correct order
dataDir = "../simulation_outputs/"
timepoint_files <- grep("average_occupancy_timepoint", list.files(path = dataDir), value=TRUE)
timepoint_files <- mixedsort(unlist(timepoint_files))
timepoint_files <- paste(dataDir, timepoint_files, sep = "")
timepoints <- map(timepoint_files, function(x) read_csv(x, col_names = FALSE))

# Create .png files of timepoints
colours = brewer.pal(9,"YlOrRd")
png(file=paste(plotsDir, "/example%03d.png", sep = ""), width=300, heigh=300)
map(timepoints, function(df) heatmap(data.matrix(df), Colv = NA, Rowv = NA, col=colours, rev=TRUE))
dev.off()

# make gif using ImageMagick

#Uxix
magickCommand <- paste("convert -delay 40 ",  plotsDir, "/*.png ", plotsDir, "/occupancy_timepoints.gif", sep = "")

system(magickCommand)


# cleanup
file.remove(paste(plotsDir, "/", list.files(path = "plots", pattern=".png"), sep = ""))

# Now create total average heatmap
average_occupany <- read_csv(paste(dataDir, "average_occupancy.txt", sep = ""), col_names = FALSE)
pdf(paste(plotsDir, "/average_occupancy.pdf", sep = ""))
heatmap(data.matrix(average_occupany), Colv = NA, Rowv = NA, col=brewer.pal(9,"YlOrRd"), rev=TRUE)
dev.off()
