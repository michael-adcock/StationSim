library(tidyverse)

# Change this to your file
file_name <- "state_data1526897238626.txt"

x_axis <- 200
y_axis <- 100
states <- read_csv(paste("../simulation_outputs/", file_name, sep=""))
subset <- filter(states, x_pos >= 60 & x_pos <= 70 & y_pos >= 55 & y_pos <= 65)


static_plot <- function(df, step_choice) {
  static <- filter(df, step==step_choice)
  plot(static$x_pos, static$y_pos, xlim=c(0, x_axis), ylim=c(y_axis, 0), axes=F)
  axis(1,at=seq(0,x_axis,10),tck=1)
  axis(2,at=seq(0,y_axis,10),tck=1)
}

step_choice <- 100
static_plot(states, step_choice)
static_plot(subset, step_choice)




counts <- right_join(count(subset, step), tibble(step=seq(0, max(states$step))))

counts
counts[is.na(counts)] <- 0L
counts
