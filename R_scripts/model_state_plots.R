library(tidyverse)
library(plotly)
library(gapminder)

# Change this to your file
file_name <- "state_data1526293477789.txt"

# Import data and sample
states <- read_csv(paste("../simulation_outputs/", file_name, sep=""))
intervals <- seq(0, max(states$step), 90)
animation_df <- filter(states, step %in% intervals)


# Static example qith base r
static <- filter(states, step==500)
plot(static$x_pos, static$y_pos, xlim=c(0,200), ylim=c(0, 100))



#ggplot version
#p <- ggplot(animation_df, aes(x_pos, y_pos, color = exit)) +
#  geom_point(aes(frame = step, ids = agent))
#p <- ggplotly(p)


# Animated plotly
p <- animation_df %>%
  plot_ly(
    x = ~x_pos,
    y = ~y_pos,
    #color = ~exit,
    frame = ~step,
    text = ~agent,
    hoverinfo = "text",
    mode = 'markers',
    type="scatter",
    ids = ~agent
  )


#non-animated plotly
aval <- list()
for(i in 1:length(intervals)) {
  a <- filter(states, step==intervals[i])
  aval[[i]] <-list(visible = FALSE,
                   #frame=a$step,
                   #exit=a$exit,
                   x=a$x_pos,
                   y=a$y_pos
                   )
}

# create steps and plot all traces
steps <- list()
p <- plot_ly()
for (i in 1:length(intervals)) {
  p <- add_markers(p, x=aval[i][[1]]$x, y=aval[i][[1]]$y, visible = aval[i][[1]]$visible,
                   marker = list(color = "blue", width = 1),
                   type = 'scatter')

  step <- list(args = list('visible', rep(FALSE, length(aval)))
  )
  step$args[[2]][i] = TRUE
  steps[[i]] = step
}

# add slider control to plot
p <- p %>%
  layout(title = "StationSim",
         sliders = list(list(active = 0,
                                 #currentvalue = list(prefix = "Step: "),
                                 steps = steps)),
                            xaxis = list(range = c(0, 200)),
                            yaxis = list(range = c(0, 100))

  )
p

#create online link - must have plotly account (and hav it set up fot your R session)
chart_link = api_create(p, filename="StationSim")
chart_link

aggregate(states, by=list(states$step), FUN=count)

sum(1,2)
